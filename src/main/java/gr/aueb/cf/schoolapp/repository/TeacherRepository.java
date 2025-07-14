package gr.aueb.cf.schoolapp.repository;

import gr.aueb.cf.schoolapp.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher,Long> {

    List<Teacher> findByRegion(Long regionId);
    Optional<Teacher> findByUuid(String uuid);
    Optional<Teacher> findByVat(String vat);

    @Query("SELECT count(t) FROM Teacher t WHERE t.uuid = :uuid")
    long getCount(String uuid);
}
