# ──────────────────────────────────────────────────────────────
# Kubernetes Deployment Guide — service-pulse-app
# ──────────────────────────────────────────────────────────────
#
# File overview:
#   k8s/
#   ├── namespace.yaml          — Dedicated namespace
#   ├── configmap.yaml          — All non-sensitive app properties
#   ├── secret.yaml             — DB + SSH credentials (TEMPLATE — edit before applying!)
#   ├── mysql-pvc.yaml          — Persistent volume for MySQL data
#   ├── mysql-deployment.yaml   — MySQL StatefulSet + headless Service
#   ├── app-deployment.yaml     — Spring Boot Deployment + ClusterIP Service
#   └── README.md               — This file
#
# ──────────────── Prerequisites ────────────────
#
# 1. Create the GHCR image pull secret (one-time):
#
#    kubectl create secret docker-registry ghcr-pull-secret \
#      --namespace=service-pulse \
#      --docker-server=ghcr.io \
#      --docker-username=rsp777 \
#      --docker-password=<YOUR_GITHUB_PAT> \
#      --docker-email=<YOUR_EMAIL>
#
# 2. Create the SSH keys secret (one-time):
#
#    kubectl create secret generic ssh-keys-secret \
#      --namespace=service-pulse \
#      --from-file=id_ed25519=/path/to/your/.ssh/id_ed25519 \
#      --from-file=known_hosts=/path/to/your/.ssh/known_hosts
#
# 3. Edit k8s/secret.yaml and replace placeholder values with
#    your base64-encoded credentials:
#      echo -n "your_value" | base64
#
# ──────────────── Deploy ────────────────
#
# Apply in order:
#
#    kubectl apply -f k8s/namespace.yaml
#    kubectl apply -f k8s/configmap.yaml
#    kubectl apply -f k8s/secret.yaml
#    kubectl apply -f k8s/mysql-pvc.yaml
#    kubectl apply -f k8s/mysql-deployment.yaml
#    kubectl apply -f k8s/app-deployment.yaml
#
# Or all at once:
#
#    kubectl apply -f k8s/
#
# ──────────────── Verify ────────────────
#
#    kubectl get pods -n service-pulse
#    kubectl logs -f deployment/service-pulse-app -n service-pulse
#
# ──────────────── Port Forward (local access) ────────────────
#
#    kubectl port-forward svc/service-pulse-app 9092:9092 -n service-pulse
#    # Open http://localhost:9092/service-pulse-app
