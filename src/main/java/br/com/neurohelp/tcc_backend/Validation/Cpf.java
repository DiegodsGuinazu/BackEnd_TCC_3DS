package br.com.neurohelp.tcc_backend.Validation;

/** Valida formato e dígitos verificadores; não consulta existência ou titularidade. */
public final class Cpf {
    private Cpf() {}
    public static boolean valido(String valor) {
        if (valor == null || !valor.matches("[0-9.\\-\\s]+")) return false;
        String cpf = valor.replaceAll("\\D", "");
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != cpf.charAt(tamanho) - '0') return false;
        }
        return true;
    }
}
