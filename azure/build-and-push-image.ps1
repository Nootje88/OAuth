# PowerShell script to build and push Docker images to Azure Container Registry
param(
    [Parameter(Mandatory = $true)]
    [string]$Environment,

    [Parameter(Mandatory = $false)]
    [string]$ResourceGroupName = "oauth-template-$Environment",

    [Parameter(Mandatory = $false)]
    [string]$Version = "latest",

    [Parameter(Mandatory = $false)]
    [string]$Source = ".",

    [Parameter(Mandatory = $false)]
    [string]$DockerfilePath = "./docker/Dockerfile.$Environment",

    [Parameter(Mandatory = $false)]
    [switch]$SkipPull = $false
)

# Check if the specified environment is valid
if ($Environment -notin @("dev", "pat", "prod")) {
    Write-Error "Invalid environment. Please specify 'dev', 'pat', or 'prod'."
    exit 1
}

# Login to Azure (if not already logged in)
$context = Get-AzContext
if (!$context) {
    Write-Host "Please login to Azure..."
    Connect-AzAccount
}

# Get the Azure Container Registry information
Write-Host "Retrieving ACR information from resource group $ResourceGroupName..."
$acr = Get-AzContainerRegistry -ResourceGroupName $ResourceGroupName

if (!$acr) {
    Write-Error "No Azure Container Registry found in resource group $ResourceGroupName."
    exit 1
}

$acrName = $acr.Name
$acrLoginServer = $acr.LoginServer

# Login to ACR
Write-Host "Logging in to ACR: $acrName..."
$credentials = Get-AzContainerRegistryCredential -ResourceGroupName $ResourceGroupName -Name $acrName
$acrPassword = $credentials.Password
$acrUsername = $credentials.Username

az acr login --name $acrName

# Build and push the image using ACR Tasks
Write-Host "Building and pushing Docker image to ACR using ACR Tasks..."
Write-Host "Environment: $Environment"
Write-Host "Version: $Version"
Write-Host "Dockerfile: $DockerfilePath"

$tags = @(
    "$acrLoginServer/oauth-template:$Version-$Environment"
)

if ($Version -eq "latest") {
    $tags += "$acrLoginServer/oauth-template:$Environment"
}

# Create tag arguments
$tagArgs = $tags | ForEach-Object { "--image $_ " }

# Build the Docker image using ACR Tasks
Write-Host "Building image with ACR Tasks..."
$buildCommand = "az acr build --registry $acrName --file $DockerfilePath $tagArgs $Source"
Write-Host "Running: $buildCommand"
Invoke-Expression $buildCommand

if ($LASTEXITCODE -ne 0) {
    Write-Error "ACR Build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "Image built and pushed successfully: $($tags -join ', ')" -ForegroundColor Green

# Optionally pull the image locally
if (!$SkipPull) {
    Write-Host "Pulling image to local Docker..."
    docker pull $tags[0]

    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Failed to pull image locally, but the build and push were successful."
    } else {
        Write-Host "Image pulled successfully: $($tags[0])"
    }
}

# Show the repository details
Write-Host "ACR repository details:"
az acr repository show --name $acrName --repository oauth-template

Write-Host "`nDocker image build and push complete!" -ForegroundColor Green
Write-Host "Image: $($tags[0])"
Write-Host "You can deploy this image to your Azure Web App using the Azure DevOps pipeline or manually."