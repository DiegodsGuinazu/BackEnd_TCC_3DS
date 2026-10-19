package br.com.neurohelp.tcc_backend.Service;

/** Valida somente formato e dígitos; não consulta existência ou titularidade na Receita. */
public final class CpfValidator {
    private CpfValidator() {}
    public static boolean valido(String valor) {
        if (valor == null || !valor.matches("[0-9.\\-\\s]+")) return false;
        String cpf = valor.replaceAll("[^0-9]", "");
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            int digito = 11 - soma % 11;
            if (digito >= 10) digito = 0;
            if (digito != cpf.charAt(tamanho) - '0') return false;
        }
        return true;
    }
}
