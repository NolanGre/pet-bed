package op.edu.ua.petbed.adoption.domain.repository;

import op.edu.ua.petbed.adoption.domain.model.AdoptionSavedPost;
import op.edu.ua.petbed.adoption.domain.model.AdoptionSavedPostId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@NullMarked
@Repository
public interface AdoptionSavedPostRepository extends JpaRepository<AdoptionSavedPost, AdoptionSavedPostId> {

    List<AdoptionSavedPost> findByUserId(Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);
}
