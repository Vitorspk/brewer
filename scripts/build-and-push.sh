#!/bin/bash
# Script to build Docker image and push to ECR

set -e

# Configuration
AWS_REGION="${AWS_REGION:-sa-east-1}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-$(aws sts get-caller-identity --query Account --output text)}"
ECR_REPOSITORY="${ECR_REPOSITORY:-brewer}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

# Full image name
IMAGE_NAME="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${IMAGE_TAG}"

echo "==================================="
echo "Building and Pushing Docker Image"
echo "==================================="
echo "AWS Region: ${AWS_REGION}"
echo "AWS Account: ${AWS_ACCOUNT_ID}"
echo "ECR Repository: ${ECR_REPOSITORY}"
echo "Image Tag: ${IMAGE_TAG}"
echo "Full Image: ${IMAGE_NAME}"
echo "==================================="

# Login to ECR
echo "Logging in to ECR..."
aws ecr get-login-password --region ${AWS_REGION} | \
    docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

# Build image
echo "Building Docker image..."
docker build -t ${ECR_REPOSITORY}:${IMAGE_TAG} .
docker tag ${ECR_REPOSITORY}:${IMAGE_TAG} ${IMAGE_NAME}

# Push image
echo "Pushing image to ECR..."
docker push ${IMAGE_NAME}

echo "==================================="
echo "✅ Image pushed successfully!"
echo "Image: ${IMAGE_NAME}"
echo "==================================="
