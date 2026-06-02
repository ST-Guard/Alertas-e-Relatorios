package school.sptech.repositorio;

import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.Query;
import school.sptech.model.RegistroAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface RegistroRepositorio extends JpaRepository<RegistroAlerta, Integer> {

    @Query("SELECT r FROM RegistroAlerta r WHERE r.issueKey = :issueKey")
    Optional<RegistroAlerta> findByIssueKey(String issueKey);

    @Query("SELECT r FROM RegistroAlerta r WHERE DATE(r.abertoEm) >= DATE(:data)")
    List<RegistroAlerta> findByAbertoEmAfter(LocalDateTime data);

    @Query(value = "SELECT idServidor, nome FROM servidor", nativeQuery = true)
    List<Object[]> buscarNomesServidores();

    boolean existsByFkServidorAndFkComponenteAndResolvidoEmIsNull(
            Integer fkServidor, Integer fkComponente);
}