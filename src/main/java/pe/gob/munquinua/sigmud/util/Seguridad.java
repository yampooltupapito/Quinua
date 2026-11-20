package pe.gob.munquinua.sigmud.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Seguridad: hash de contraseñas con PBKDF2 (HMAC-SHA256, sal aleatoria), verificación en tiempo constante,
 * reconocimiento de los hashes antiguos (SHA-256 sin sal), contraseñas temporales y tokens aleatorios (CSRF).
 *
 * Formato guardado: "pbkdf2$iteraciones$salBase64$hashBase64".
 */
public final class Seguridad {

    /** Iteraciones de PBKDF2 para los hashes nuevos (las guarda cada hash, así se pueden subir sin romper los viejos). */
    public static final int ITERACIONES = 120_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2";
    /** Límites razonables para las iteraciones de un hash guardado (evita datos corruptos o costos absurdos). */
    private static final int MIN_ITERACIONES = 1_000;
    private static final int MAX_ITERACIONES = 1_000_000;
    private static final Pattern HASH_LEGADO = Pattern.compile("^[0-9a-fA-F]{64}$");

    // Contraseñas temporales: sin caracteres que se confunden (0/O, 1/l/I)
    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final int LARGO_TEMPORAL = 12;

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_TOKEN = 32;

    private Seguridad() { }

    // ---------------------------------------------------------------- contraseñas (PBKDF2)

    /** Hash nuevo de una contraseña: PBKDF2-HMAC-SHA256, 120000 iteraciones y sal aleatoria de 16 bytes. */
    public static String hashear(String contrasena) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(contrasena, sal, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Comprueba una contraseña contra lo guardado. Acepta AMBOS formatos: el nuevo (PBKDF2) y el antiguo
     * (SHA-256 sin sal, 64 hexadecimales). La comparación es en tiempo constante. Un formato inválido devuelve
     * false (nunca lanza excepción).
     */
    public static boolean verificar(String contrasena, String almacenado) {
        if (contrasena == null || almacenado == null) {
            return false;
        }
        try {
            if (esLegado(almacenado)) {
                return iguales(sha256(contrasena).getBytes(StandardCharsets.UTF_8),
                        almacenado.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            }
            return verificarPbkdf2(contrasena, almacenado);
        } catch (RuntimeException e) {   // Base64 o número mal formado, parámetros inválidos...
            return false;
        }
    }

    /** true si es un hash antiguo: SHA-256 sin sal en 64 caracteres hexadecimales. */
    public static boolean esLegado(String almacenado) {
        return almacenado != null && HASH_LEGADO.matcher(almacenado.trim()).matches();
    }

    private static boolean verificarPbkdf2(String contrasena, String almacenado) {
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        int iteraciones = Integer.parseInt(partes[1]);
        if (iteraciones < MIN_ITERACIONES || iteraciones > MAX_ITERACIONES) {
            return false;
        }
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        if (sal.length == 0 || esperado.length == 0) {
            return false;
        }
        byte[] calculado = derivar(contrasena, sal, iteraciones, esperado.length * 8);
        return iguales(calculado, esperado);
    }

    private static byte[] derivar(String contrasena, byte[] sal, int iteraciones) {
        return derivar(contrasena, sal, iteraciones, BITS_HASH);
    }

    private static byte[] derivar(String contrasena, byte[] sal, int iteraciones, int bits) {
        char[] clave = contrasena.toCharArray();
        PBEKeySpec especificacion = new PBEKeySpec(clave, sal, iteraciones, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        } finally {
            especificacion.clearPassword();
            java.util.Arrays.fill(clave, '\0');
        }
    }

    private static boolean iguales(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);   // tiempo constante
    }

    // ---------------------------------------------------------------- SHA-256 (solo para hashes antiguos)

    /** SHA-256 en hexadecimal minúscula (64 caracteres). Solo se usa para reconocer los hashes antiguos. */
    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    // ---------------------------------------------------------------- contraseñas temporales y tokens

    /**
     * Contraseña temporal de 12 caracteres (SecureRandom) que cumple la política: al menos una mayúscula,
     * una minúscula y un número. No se escribe en el log ni se guarda en texto plano.
     */
    public static String generarPasswordTemporal() {
        String todos = MAYUSCULAS + MINUSCULAS + NUMEROS;
        char[] c = new char[LARGO_TEMPORAL];
        c[0] = elegir(MAYUSCULAS);
        c[1] = elegir(MINUSCULAS);
        c[2] = elegir(NUMEROS);
        for (int i = 3; i < LARGO_TEMPORAL; i++) {
            c[i] = elegir(todos);
        }
        for (int i = c.length - 1; i > 0; i--) {   // mezcla (Fisher-Yates) para que las 3 obligatorias no queden al inicio
            int j = ALEATORIO.nextInt(i + 1);
            char t = c[i];
            c[i] = c[j];
            c[j] = t;
        }
        return new String(c);
    }

    private static char elegir(String alfabeto) {
        return alfabeto.charAt(ALEATORIO.nextInt(alfabeto.length()));
    }

    /** Token aleatorio de 32 bytes en Base64 URL (sin relleno), apto para usar como token CSRF. */
    public static String tokenAleatorio() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
