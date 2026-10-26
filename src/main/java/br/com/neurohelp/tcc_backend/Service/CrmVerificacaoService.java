package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class CrmVerificacaoService {
    private final profissionalRepository profissionais;
    private final CrmService crm;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    public CrmVerificacaoService(profissionalRepository profissionais, CrmService crm) {
        this.profissionais = profissionais; this.crm = crm;
    }
    public record Solicitacao(int id, String nome, String numRegistro, Instant solicitadoEm, long revisao) {}
    @Transactional
    public CrmService.Verificacao solicitar(int id) {
        var p = buscarComBloqueio(id);
        if (!CrmService.eCrm(p.getNumRegistro())) throw new CrmService.ConsultaException(HttpStatus.BAD_REQUEST,
                "Informe um registro CRM com UF no seu perfil. Outros conselhos não são verificados pelo CFM.");
        crm.validarRegistroSeCrm(p.getNumRegistro(), p.getNome());
        String atual = CrmService.verificacao(p).status();
        if ("EM_ANALISE".equals(atual) || "VERIFICADO".equals(atual)) return CrmService.verificacao(p);
        p.setCrmStatus("EM_ANALISE");
        p.setCrmNomeConferido(p.getNome()); p.setCrmRegistroConferido(p.getNumRegistro());
        p.setCrmSolicitadoEm(Instant.now()); p.setCrmConferidoEm(null); p.setCrmRevisorId(null);
        p.setCrmRevisao(p.getCrmRevisao() + 1);
        profissionais.save(p);
        return CrmService.verificacao(p);
    }
    @Transactional(readOnly = true)
    public Page<Solicitacao> listar(int pagina) {
        return profissionais.findByCrmStatusOrderByCrmSolicitadoEmAsc("EM_ANALISE", PageRequest.of(Math.max(0, pagina), 25))
                .map(p -> new Solicitacao(p.getId(), p.getNome(), p.getNumRegistro(), p.getCrmSolicitadoEm(), p.getCrmRevisao()));
    }
    @Transactional
    public CrmService.Verificacao revisar(int id, long revisao, boolean confirmado, long revisorId) {
        var p = buscarComBloqueio(id);
        if (!"EM_ANALISE".equals(CrmService.verificacao(p).status()) || revisao != p.getCrmRevisao())
            throw new CrmService.ConsultaException(HttpStatus.CONFLICT, "O pedido foi alterado. Atualize a lista antes de conferir.");
        p.setCrmStatus(confirmado ? "VERIFICADO" : "NAO_CONFIRMADO");
        p.setCrmConferidoEm(Instant.now()); p.setCrmRevisorId(revisorId);
        p.setCrmRevisao(p.getCrmRevisao() + 1);
        profissionais.save(p);
        return CrmService.verificacao(p);
    }
    private UserProf buscarComBloqueio(int id) {
        var profissional = profissionais.buscarParaConferencia(id).orElseThrow(() -> new CrmService.ConsultaException(HttpStatus.NOT_FOUND, "Profissional não encontrado."));
        // O filtro JWT pode ter carregado a entidade antes do lock; recarregar evita aprovação com estado antigo.
        entityManager.refresh(profissional, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return profissional;
    }
}
