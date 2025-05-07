# Apache OFBiz – Migration vers Microservices sur GKE

Ce projet a pour objectif de migrer une application ERP monolithique (Apache OFBiz) vers une architecture microservices déployée sur Google Kubernetes Engine (GKE).

## 🎯 Objectifs

- Découper les modules fonctionnels d'OFBiz en microservices
- Conteneuriser chaque service avec Docker
- Pousser les images sur Docker Hub
- Déployer l’architecture sur GKE avec YAML et GitHub Actions

## 🧱 Microservices prévus

| Microservice        | Description                                      | API prévue         |
|---------------------|--------------------------------------------------|---------------------|
| `product-service`   | Gestion des produits, catégories, inventaire     | REST CRUD / Webhooks |
| `order-service`     | Prise de commande, validation, expédition        | REST / PubSub        |
| `accounting-service`| Factures, paiements, écritures comptables        | REST / events        |
| `auth-service`      | Gestion des utilisateurs, authentification       | OAuth2 / JWT         |
| `catalog-service`   | Recherche catalogue, navigation, filtres         | REST / Kafka events  |

## 🐳 Docker

Chaque microservice est packagé dans une image Docker disponible sur Docker Hub :

📦 https://hub.docker.com/u/vareladavid

## ☁️ Déploiement GKE

Utilisation de :
- Google Cloud CLI (`gcloud`)
- Kubernetes (`kubectl`)
- YAML (Deployments, Services, Ingress)
- Helm (optionnel)
- CI/CD avec GitHub Actions
- Monitoring : Prometheus + Grafana
- Sécurité : IAM, RBAC, Secrets chiffrés

## 🔧 Structure du dépôt


## 🧠 Formation & Contexte

Projet réalisé dans le cadre de la formation **Google Kubernetes Engine - Architecting**, avec pour but d'apprendre à :
- Déployer des conteneurs sur GKE
- Refactoriser une application monolithique
- Automatiser le déploiement avec CI/CD

## 📌 Auteur

David VARELA  
🔗 [GitHub](https://github.com/VARELAdavidhugo)  
🐳 [Docker Hub](https://hub.docker.com/u/vareladavid)



