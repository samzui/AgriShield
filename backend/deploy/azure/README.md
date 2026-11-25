# Azure AKS deployment
Build and push the backend image to Azure Container Registry, create an AKS cluster with at least two replicas, configure managed PostgreSQL, Redis, Key Vault and an HTTPS ingress, then apply `../kubernetes/deployment.yaml` with environment-specific secrets.
