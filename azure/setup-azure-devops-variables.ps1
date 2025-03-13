# PowerShell script to set up Azure DevOps variable groups for OAuth template

param(
    [Parameter(Mandatory = $true)]
    [string]$Organization,

    [Parameter(Mandatory = $true)]
    [string]$Project,

    [Parameter(Mandatory = $false)]
    [string]$DeploymentOutputsFile = $null,

    [Parameter(Mandatory = $false)]
    [string]$Environment = "dev",

    [Parameter(Mandatory = $false)]
    [hashtable]$CustomVariables = @{}
)

# Check Azure CLI and DevOps extension
Write-Host "Checking Azure CLI and DevOps extension..."
$cliVersion = az version --query '"azure-cli"' -o tsv
if (-not $cliVersion) {
    Write-Error "Azure CLI not found. Please install it first: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"
    exit 1
}

$extensionCheck = az extension list --query "[?name=='azure-devops'].name" -o tsv
if (-not $extensionCheck) {
    Write-Host "Installing Azure DevOps CLI extension..."
    az extension add --name azure-devops
}

# Login to Azure DevOps
Write-Host "Logging in to Azure DevOps..."
az devops configure --defaults organization="https://dev.azure.com/$Organization" project="$Project"

# Variable group names
$globalVariableGroupName = "oauth-global-variables"
$envVariableGroupName = "oauth-$Environment-variables"

# Check if deployment outputs file exists and load it
$deploymentOutputs = $null
if ($DeploymentOutputsFile -and (Test-Path $DeploymentOutputsFile)) {
    Write-Host "Loading deployment outputs from $DeploymentOutputsFile..."
    $deploymentOutputs = Get-Content $DeploymentOutputsFile | ConvertFrom-Json
}

# Set up global variables
Write-Host "Setting up global variable group: $globalVariableGroupName"

# Check if global variable group exists
$globalVarGroup = az pipelines variable-group list --query "[?name=='$globalVariableGroupName']" -o json | ConvertFrom-Json
if ($globalVarGroup.Count -eq 0) {
    # Create new variable group
    Write-Host "Creating new global variable group..."

    $globalVariables = @{
        "MavenGroupId" = "com.template";
        "MavenArtifactId" = "OAuth";
        "ImageRepository" = "oauth-template";
        "ContainerRegistry" = if ($deploymentOutputs) { $deploymentOutputs.containerRegistryUrl.value } else { "yourregistry.azurecr.io" };
    }

    # Add custom variables
    foreach ($key in $CustomVariables.Keys) {
        if ($key -like "Global*") {
            $varName = $key -replace "^Global", ""
            $globalVariables[$varName] = $CustomVariables[$key]
        }
    }

    # Create variable group
    $varGroupJson = ConvertTo-Json -Compress @{
        name = $globalVariableGroupName;
        variables = $globalVariables;
        type = "Vsts";
    }

    az pipelines variable-group create --name $globalVariableGroupName --variables $varGroupJson
} else {
    Write-Host "Global variable group already exists. Updating variables..."
    $groupId = $globalVarGroup[0].id

    # Update existing variables
    if ($deploymentOutputs) {
        az pipelines variable-group variable update --group-id $groupId --name "ContainerRegistry" --value $deploymentOutputs.containerRegistryUrl.value
    }

    # Add custom variables
    foreach ($key in $CustomVariables.Keys) {
        if ($key -like "Global*") {
            $varName = $key -replace "^Global", ""
            az pipelines variable-group variable update --group-id $groupId --name $varName --value $CustomVariables[$key]
        }
    }
}

# Set up environment-specific variables
Write-Host "Setting up environment variable group: $envVariableGroupName"

# Check if environment variable group exists
$envVarGroup = az pipelines variable-group list --query "[?name=='$envVariableGroupName']" -o json | ConvertFrom-Json
if ($envVarGroup.Count -eq 0) {
    # Create new variable group
    Write-Host "Creating new environment variable group..."

    $envVariables = @{
        "AzureSubscription" = "Azure";
        "ResourceGroupName" = "oauth-template-$Environment";
        "WebAppName" = if ($deploymentOutputs) { $deploymentOutputs.webAppName.value } else { "oauth-app-$Environment" };
        "MysqlServerName" = if ($deploymentOutputs) { $deploymentOutputs.mysqlServerFqdn.value.Split('.')[0] } else { "oauth-mysql-$Environment" };
        "DbUsername" = "oauthadmin";
        "DbPassword" = "$(DbPassword)"; # Secret reference
        "DbUrl" = if ($deploymentOutputs) { "jdbc:mysql://$($deploymentOutputs.mysqlServerFqdn.value):3306/OAuthTemplate_$(($Environment).ToUpper())?useSSL=true&serverTimezone=UTC" } else { "jdbc:mysql://localhost:3306/oauth" };
        "AppBaseUrl" = if ($deploymentOutputs) { $deploymentOutputs.webAppUrl.value } else { "https://localhost:8080" };
    }

    # Add environment-specific custom variables
    foreach ($key in $CustomVariables.Keys) {
        if ($key -notlike "Global*") {
            $envVariables[$key] = $CustomVariables[$key]
        }
    }

    # Create variable group
    $varGroupJson = ConvertTo-Json -Compress @{
        name = $envVariableGroupName;
        variables = $envVariables;
        type = "Vsts";
    }

    az pipelines variable-group create --name $envVariableGroupName --variables $varGroupJson

    # Set secret variables
    if ($envVarGroup.Count -gt 0) {
        $groupId = $envVarGroup[0].id

        # Prompt for secret variables
        $dbPassword = Read-Host "Enter database password for $Environment environment" -AsSecureString
        $jwtSecret = Read-Host "Enter JWT secret for $Environment environment" -AsSecureString
        $googleClientId = Read-Host "Enter Google OAuth Client ID for $Environment environment"
        $googleClientSecret = Read-Host "Enter Google OAuth Client Secret for $Environment environment" -AsSecureString

        # Convert secure strings to plain text for Azure DevOps CLI
        $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPassword)
        $dbPasswordPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)

        $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($jwtSecret)
        $jwtSecretPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)

        $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($googleClientSecret)
        $googleClientSecretPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)

        # Set secret variables
        az pipelines variable-group variable create --group-id $groupId --name "DbPassword" --value $dbPasswordPlain --secret true
        az pipelines variable-group variable create --group-id $groupId --name "JwtSecret" --value $jwtSecretPlain --secret true
        az pipelines variable-group variable create --group-id $groupId --name "GoogleClientId" --value $googleClientId
        az pipelines variable-group variable create --group-id $groupId --name "GoogleClientSecret" --value $googleClientSecretPlain --secret true
    }
} else {
    Write-Host "Environment variable group already exists. Updating variables..."
    $groupId = $envVarGroup[0].id

    # Update existing variables from deployment outputs
    if ($deploymentOutputs) {
        az pipelines variable-group variable update --group-id $groupId --name "WebAppName" --value $deploymentOutputs.webAppName.value
        az pipelines variable-group variable update --group-id $groupId --name "MysqlServerName" --value $deploymentOutputs.mysqlServerFqdn.value.Split('.')[0]
        az pipelines variable-group variable update --group-id $groupId --name "DbUrl" --value "jdbc:mysql://$($deploymentOutputs.mysqlServerFqdn.value):3306/OAuthTemplate_$(($Environment).ToUpper())?useSSL=true&serverTimezone=UTC"
        az pipelines variable-group variable update --group-id $groupId --name "AppBaseUrl" --value $deploymentOutputs.webAppUrl.value
    }

    # Add custom variables
    foreach ($key in $CustomVariables.Keys) {
        if ($key -notlike "Global*") {
            Write-Host "Updating variable: $key"
            # Check if the variable is a secret
            $isSecret = $key -in @("DbPassword", "JwtSecret", "GoogleClientSecret", "EmailPassword")
            az pipelines variable-group variable update --group-id $groupId --name $key --value $CustomVariables[$key] --secret:$isSecret
        }
    }
}

Write-Host "Variable groups setup completed for $Environment environment." -ForegroundColor Green
Write-Host "Make sure to securely store any sensitive information in a password manager."

# Output information about the next steps
Write-Host "`nNext steps:" -ForegroundColor Yellow
Write-Host "1. Configure the OAuth application with the Web App URL for redirects"
Write-Host "2. Create or import the Azure Pipeline using the azure-pipelines.yml file"
Write-Host "3. Run the pipeline to deploy the application"