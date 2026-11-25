# GCP GKE deployment
Build and push the backend image to Artifact Registry, run at least two replicas on GKE, use Cloud SQL PostgreSQL, Secret Manager, Memorystore/Redis and an HTTPS load balancer, then apply `../kubernetes/deployment.yaml` with environment-specific secrets.
