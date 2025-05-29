package fr.mineria.azauth.demo;

import javax.swing.JOptionPane;
import fr.mineria.azauth.AzAuth;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Demo {

	public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        
        parser.allowsUnrecognizedOptions();
        
        parser.accepts("username").withRequiredArg();
        parser.accepts("password").withRequiredArg();
        parser.accepts("accessToken").withRequiredArg();

        OptionSet options = parser.parse(args);

		
        AzAuth azAuth = AzAuth.withBaseUrl("https://yourwebsite.fr/").debug(true);
        AuthResult auth = AuthManager.performAuthentication(options, azAuth);
        AzAuth.User currentUser = AuthManager.getCurrentUser();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(currentUser != null) azAuth.logout(currentUser.getAccessToken());
                System.out.println("✅ Déconnexion effectuée.");
            } catch (Exception e) {
                System.err.println("Erreur pendant la déconnexion : " + e.getMessage());
            }
        }));
        
        switch (auth.getStatus()) {
            case SUCCESS:
            	System.out.println(
            		    "\n\u2705 Connexion réussie !\n" +
            		    "Bienvenue, " + auth.getUser().getUsername() + " (" + auth.getUser().getUuid() + ")\n" +
            		    "Type d'authentification : " + auth.getType() + "\n"
            		);
            	break;
            case FAIL:
            	JOptionPane.showMessageDialog(
            		    null,
            		    "Erreur d'authentification :\n" + auth.getErrorMessage()
            		        + "\n\nVotre session est invalide, expirée ou un problème est survenu.\n"
            		        + "Veuillez redémarrer le jeu.\n"
            		        + "Si le problème persiste, contactez-nous sur discord.yourwebsite.fr",
            		    "Erreur d'authentification",
            		    JOptionPane.ERROR_MESSAGE
            		);
            	System.exit(0);
                break;
            case EMAIL_UNVERIFIED:
                JOptionPane.showMessageDialog(
                    null,
                    "Votre email n'est pas vérifié.\n" +
                    "Veuillez vérifier vos mails et valider votre adresse email, puis relancez le jeu.",
                    "Email non vérifié",
                    JOptionPane.WARNING_MESSAGE
                );
            	System.exit(0);
                break;
            default:
                JOptionPane.showMessageDialog(
                    null,
                    "Une erreur inconnue est survenue lors de l'authentification.\n" +
                    "Veuillez redémarrer le jeu.\n" +
                    "Si le problème persiste, contactez-nous sur discord.yourwebsite.fr",
                    "Erreur d'authentification",
                    JOptionPane.ERROR_MESSAGE
                );
                System.exit(0);
                break;
        }		
	}
}
