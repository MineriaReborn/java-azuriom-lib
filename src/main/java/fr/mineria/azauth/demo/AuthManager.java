package fr.mineria.azauth.demo;

import java.io.IOException;
import javax.swing.JOptionPane;
import fr.mineria.azauth.AzAuth;
import joptsimple.OptionSet;
import lombok.Getter;

public class AuthManager {

    private static @Getter AzAuth.User currentUser;
	
    public static AuthResult performAuthentication(OptionSet options, AzAuth azAuth) {
    	
        final String email = (String) options.valueOf("username");
        final String password = options.has("password") ? (String) options.valueOf("password") : null;
        final String accessToken = options.has("accessToken") ? (String) options.valueOf("accessToken") : null;

        try {
            if (password != null) {
                System.out.println("Authentification en cours...");
                return authenticateWithPassword(email, password, azAuth);
            } else if (accessToken != null) {
                System.out.println("Authentification par accessToken en cours...");
                currentUser = azAuth.verify(accessToken);
                return checkEmailVerification(currentUser, AuthType.ACCESS_TOKEN);
            } else {
                return AuthResult.failure(AuthType.UNKNOWN, "Aucune méthode d'authentification disponible.");
            }
        } catch (AzAuth.AzAuthException | IOException ex) {
            return AuthResult.failure(
                password != null ? AuthType.PASSWORD : AuthType.ACCESS_TOKEN,
                ex.getMessage()
            );
        }
    }

    private static AuthResult authenticateWithPassword(String email, String password, AzAuth azAuth) throws IOException {
        try {
        	currentUser = azAuth.authenticate(email, password);
            return checkEmailVerification(currentUser, AuthType.PASSWORD);
        } catch (AzAuth.AzAuthException ex) {
            if (isPending2FA(ex)) {
                System.out.println("Attente du code 2FA...");
                return handle2FA(email, password, azAuth);
            }
            return AuthResult.failure(AuthType.PASSWORD, ex.getMessage());
        }
    }

    private static AuthResult handle2FA(String email, String password, AzAuth azAuth) throws IOException {
        while (true) {
            String code2fa = promptFor2FA(email);
            if (code2fa == null || code2fa.trim().isEmpty()) {
                return AuthResult.failure(AuthType.PASSWORD, "2FA annulé par l'utilisateur.");
            }
            try {
            	currentUser = azAuth.authenticate(email, password, code2fa);
                return checkEmailVerification(currentUser, AuthType.PASSWORD);
            } catch (AzAuth.AzAuthException ex2) {
                if (isPending2FA(ex2)) {
                    show2FAErrorDialog();
                    System.out.println("Attente d'un nouveau code 2FA...");
                } else {
                    return AuthResult.failure(AuthType.PASSWORD, ex2.getMessage());
                }
            }
        }
    }

    private static boolean isPending2FA(AzAuth.AzAuthException ex) {
        return "pending".equalsIgnoreCase(ex.getStatus()) && "2fa".equalsIgnoreCase(ex.getReason());
    }

    private static void show2FAErrorDialog() {
        JOptionPane.showMessageDialog(
            null,
            "Code 2FA incorrect. Veuillez réessayer.",
            "Erreur 2FA",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static String promptFor2FA(String username) {
        return JOptionPane.showInputDialog(
            null,
            "Entrez le code 2FA pour " + username + " :",
            "Authentification à deux facteurs",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    private static AuthResult checkEmailVerification(AzAuth.User user, AuthType type) {
        if (user == null) {
            return AuthResult.failure(type, "Utilisateur non trouvé.");
        }
        if (!user.isEmailVerified()) {
            return new AuthResult(AuthStatus.EMAIL_UNVERIFIED, type, user, "L'email de l'utilisateur n'est pas vérifié.");
        }
        return new AuthResult(AuthStatus.SUCCESS, type, user, null);
    }
}