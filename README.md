# Java Azuriom Lib

Une bibliothèque Java légère et fluide pour interagir avec l'API d'authentification du CMS [Azuriom](https://azuriom.com/). Contrairement à la bibliothèque officielle [AzAuth](https://github.com/Azuriom/AzAuth), cette librairie se concentre uniquement sur l'utilisation de l'API d'authentification d'Azuriom, sans inclure de fonctionnalités avancées ou de dépendances complexes.

## Caractéristiques

- **Simple et minimaliste** : Une seule classe pour interagir avec l'API.
- **Fluide et portable** : Conçue pour être légère et facile à intégrer dans vos projets.
- **Pas de gestion complexe** : Fournit uniquement les outils nécessaires pour utiliser l'API d'authentification d'Azuriom.

## Installation

Ajoutez la class **AzAuth.java** à votre projet.

## Exemple d'utilisation

Voici un exemple simple pour démontrer l'utilisation de la bibliothèque :

## Exemple d'utilisation

Voici comment utiliser la bibliothèque `AzAuth` pour interagir avec l'API d'authentification d'Azuriom :

```java
import fr.mineria.azauth.AzAuth;

public class Demo {
    public static void main(String[] args) {
        // Initialisation de l'authentification avec l'URL de base du site Azuriom
        AzAuth azAuth = AzAuth.withBaseUrl("https://votre-site-azuriom.com")
                              .debug(true); // Active le mode debug (facultatif)

        try {
            // Étape 1 : Authentification avec email et mot de passe
            AzAuth.User user = azAuth.authenticate("email@example.com", "votre-mot-de-passe");

            // Affichage des informations utilisateur après authentification
            System.out.println("Authentification réussie !");
            System.out.println("Utilisateur : " + user);

            // Étape 2 : Vérification du token d'accès
            AzAuth.User verifiedUser = azAuth.verify(user.getAccessToken());
            System.out.println("Vérification réussie !");
            System.out.println("Utilisateur vérifié : " + verifiedUser);

            // Étape 3 : Déconnexion
            azAuth.logout(user.getAccessToken());
            System.out.println("Déconnexion réussie !");
        } catch (AzAuth.AzAuthException e) {
            // Gestion des erreurs spécifiques à AzAuth
            System.err.println("Erreur AzAuth : " + e.getMessage());
        } catch (IOException e) {
            // Gestion des erreurs réseau ou d'entrée/sortie
            System.err.println("Erreur réseau : " + e.getMessage());
        }
    }
}
```

## Contributions

Les contributions sont les bienvenues. Si vous avez des idées ou des suggestions, n'hésitez pas à ouvrir une issue ou à soumettre une pull request.

## Licence

Cette bibliothèque est sous licence MIT. Consultez le fichier `LICENSE` pour plus d'informations.
