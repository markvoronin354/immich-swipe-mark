# Immich Swipe

![GitHub release (latest by date)](https://img.shields.io/github/v/release/markvoronin354/immich-swipe-android)
![GitHub License](https://img.shields.io/github/license/markvoronin354/immich-swipe-android)
![Kotlin](https://img.shields.io/badge/langage-Kotlin-purple)
![Android](https://img.shields.io/badge/plateforme-Android-green)

[English version available here](README.md) | [Politique de confidentialité](PRIVACY_POLICY.md)

Immich Swipe est une application Android open-source conçue pour faciliter le tri de vos photos et vidéos hébergées sur votre serveur [Immich](https://immich.app/).

Ce projet est un fork basé sur le projet original [Minos2020/immich-swipe-android](https://github.com/Minos2020/immich-swipe-android).

> **Avertissement** : Ce projet est indépendant et n'est affilié d'aucune façon avec le projet officiel Immich.

## 📸 Aperçu

|                                         Écran d'accueil                                         |                                     La pile de tri                                      |                                        Mode Revue                                        |
|:-----------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/10_Light_HomeScreen_Listview.jpg" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/09_Dark_SwipeScreen.jpg" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/11_Dark_ReviewScreen.jpg" width="200"> |
|                                     *Parcourez vos albums*                                      |                                  *Swipez pour décider*                                  |                              *Vérifiez avant de supprimer*                               |

<details>
<summary>🌙 <b>Voir la galerie en Mode Sombre</b></summary>

|                                        Écran d'accueil                                         |                                     La pile de tri                                     |                                       Mode Revue                                        |                                       Paramètres                                       |
|:----------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/05_Dark_HomeScreen_GridView.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/07_Dark_SwipeScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/08_Dark_ReviewScreen.jpg" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Dark_SettingsScreen.png" width="200"> |

</details>

## ✨ Fonctionnalités

- **🚀 Pile de tri Rapide** : Swipez à droite pour garder, à gauche pour supprimer, ou vers le haut pour des actions personnalisées (Archiver, Favori, Verrouiller, Ajouter à un album). Les médias triés disparaissent de la timeline en temps réel.
- **👯 Recherche et Gestion des Doublons** : Détectez et gérez vos doublons photo avec comparaison côte à côte, prévisualisation zoomée, lecture vidéo et analyse des détails de format.
- **📁 Collections (Albums Virtuels)** : Accédez à des groupes spéciaux comme **TOUS**, **Orphelins** (photos hors album), et **Doublons**.
- **📊 Statistiques d'Utilisation** : Visualisez votre progression et le détail de vos actions de tri.
- **🛡️ Mode Revue Avancé** : Vérifiez vos décisions (Supprimer, Garder, Archiver, Favori, Verrouiller) et estimez l'espace libéré avant la synchronisation avec votre serveur Immich.
- **⚙️ Actions et Gestes Personnalisables** : Personnalisez vos boutons d'action, les seuils de balayage, les retours vibratoires, l'avancement automatique et la lecture auto des vidéos.
- **🔄 Support Multi-Comptes** : Changez d'utilisateur facilement ; vos décisions locales et votre progression sont sauvegardées séparément pour chaque compte.
- **🗃️ Gestion de la Base de Données et Cache** : Exportez, importez ou videz votre base locale (format JSON) et gérez le cache d'images et de vidéos.
- **🚦 Diagnostic de Connexion** : Indicateur de statut en temps réel, support HTTP et IP directes, et accès aux journaux (logs) intégrés.
- **🎨 Interface Moderne** : Développée avec Jetpack Compose et Material Design 3, disponible en Français, Anglais et Espagnol.

## ⚙️ Configuration & Permissions de la Clé API

1. Entrez l'URL ou l'adresse IP avec le port de votre serveur Immich (ex: `https://immich.votre-domaine.fr` ou `http://10.0.0.10:2283`).
2. Entrez votre clé API Immich ([créez-en une sur Immich dans Paramètres utilisateur > Clés API](https://my.immich.app/user-settings?isOpen=api-keys)).

### 🔐 Permissions de la Clé API

Afin d'assurer le bon fonctionnement tout en respectant le **Principe de Moindre Privilège**, configurez votre clé API avec les permissions suivantes sur votre serveur Immich :

#### **Permissions Requises**
- `user.read` – Récupérer les informations du profil utilisateur (`/api/users/me`).
- `album.read` – Récupérer la liste de vos albums (`/api/albums`).
- `asset.read` / `asset.view` – Parcourir vos médias, miniatures et métadonnées (`/api/assets/{id}`).
- `search.read` – Rechercher les médias dans vos albums/collections et calculer les statistiques (`/api/search/metadata`, `/api/search/statistics`).
- `asset.delete` – Déplacer les médias supprimés vers la corbeille Immich lors de la synchronisation (`/api/assets`).
- `asset.download` – Afficher les photos en haute résolution et lire les vidéos de manière fluide.

#### **Permissions Optionnelles (Selon les fonctionnalités utilisées)**
- `duplicate.read` – **Requis pour la fonctionnalité Doublons** : Permet de scanner et résoudre les clusters de photos dupliquées (`/api/duplicates`).
- `asset.update` – **Requis pour les Actions** : Permet d'archiver, de mettre en favori, de verrouiller ou d'éditer/pivoter des médias (`/api/assets`, `/api/assets/{id}/edits`).
- `album.asset.add` / `album.update` – **Requis pour "Ajouter à un album"** : Permet d'ajouter directement un média à un album existant (`/api/albums/{id}/assets`).
- `userProfileImage.read` – Permet d'afficher la photo de profil dans le menu de changement de compte (`/api/users/me`).

3. Sélectionnez un album ou une collection virtuelle et commencez à trier !

## 📦 Installation

|                                                                                                                       **Orion Store**                                                                                                                        |   **Téléchargement Direct**   |  **IzzyOnDroid / F-Droid**    |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------:|:----------------------------:|
| [<img src="https://github.com/RookieEnough/Orion-Store/blob/main/assets/graphics/orion-badge.png?raw=true"  alt="Get it on Orion Store" height="50">](https://rookieenough.github.io/Orion-Data/redirect.html?id=immich-swipe-android) | Récupérez le dernier APK dans la section [Releases](https://github.com/markvoronin354/immich-swipe-android/releases)   |  Prochainement disponible |

## 🛠️ Build

Si vous souhaitez compiler l'application vous-même :

- **JDK 17** ou supérieur requis.
- **Android Studio** (Version Ladybug ou plus récente recommandée).
- Clonez le dépôt et importez le projet dans Android Studio.
- Synchronisez Gradle.
- Utilisez `./gradlew assembleDebug` pour générer un APK de test.

## 📄 Licence

Ce projet est sous licence GNU GPL v3. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## ⚖️ Responsabilité

Bien que ce projet soit développé avec le plus grand soin et testé régulièrement, je ne peux garantir la sécurité absolue de vos données. En utilisant Immich Swipe, vous acceptez que l'auteur ne puisse être tenu responsable en cas de perte de données ou de suppression involontaire de médias.

Il est important de notation que :
- **Sécurité de la corbeille** : Immich Swipe ne vide jamais définitivement la corbeille de votre serveur Immich. Vos éléments supprimés sont envoyés vers la corbeille Immich et restent récupérables via l'interface officielle Immich pendant la durée de conservation configurée de la corbeille.
- **Principe de moindre privilège** : Afin de minimiser les risques, il est vivement conseillé de ne configurer votre clé API qu'avec les permissions strictement nécessaires listées dans la section [Configuration & Permissions de la Clé API](#-configuration--permissions-de-la-clé-api).
