package com.school.counseling.module.feed.repository;

import com.school.counseling.module.feed.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.department " +
           "LEFT JOIN FETCH p.author a " +
           "LEFT JOIN FETCH a.role " +
           "LEFT JOIN FETCH a.department " +
           "WHERE p.id = :id AND p.isDeleted = false")
    Optional<Post> findByIdWithDetails(@Param("id") Long id);

    @Query(value = "SELECT p FROM Post p " +
                   "LEFT JOIN FETCH p.department " +
                   "LEFT JOIN FETCH p.author a " +
                   "LEFT JOIN FETCH a.role " +
                   "LEFT JOIN FETCH a.department " +
                   "WHERE p.postType = :postType AND p.status = 'APPROVED' AND p.isDeleted = false " +
                   "ORDER BY p.isPinned DESC, p.createdAt DESC",
           countQuery = "SELECT count(p) FROM Post p WHERE p.postType = :postType AND p.status = 'APPROVED' AND p.isDeleted = false")
    Page<Post> findApprovedPostsByType(@Param("postType") String postType, Pageable pageable);

    @Query(value = "SELECT p FROM Post p " +
                   "LEFT JOIN FETCH p.department " +
                   "LEFT JOIN FETCH p.author a " +
                   "LEFT JOIN FETCH a.role " +
                   "LEFT JOIN FETCH a.department " +
                   "WHERE p.postType = 'OFFICIAL_ANNOUNCEMENT' AND p.department.id = :deptId AND p.status = 'APPROVED' AND p.isDeleted = false " +
                   "ORDER BY p.isPinned DESC, p.createdAt DESC",
           countQuery = "SELECT count(p) FROM Post p WHERE p.postType = 'OFFICIAL_ANNOUNCEMENT' AND p.department.id = :deptId AND p.status = 'APPROVED' AND p.isDeleted = false")
    Page<Post> findApprovedOfficialPostsByDepartment(@Param("deptId") Long deptId, Pageable pageable);

    @Query(value = "SELECT p FROM Post p " +
                   "LEFT JOIN FETCH p.department " +
                   "LEFT JOIN FETCH p.author a " +
                   "LEFT JOIN FETCH a.role " +
                   "LEFT JOIN FETCH a.department " +
                   "WHERE p.postType = 'OFFICIAL_ANNOUNCEMENT' AND p.status = 'APPROVED' AND p.isDeleted = false AND " +
                   "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "ORDER BY p.isPinned DESC, p.createdAt DESC",
           countQuery = "SELECT count(p) FROM Post p WHERE p.postType = 'OFFICIAL_ANNOUNCEMENT' AND p.status = 'APPROVED' AND p.isDeleted = false AND " +
                        "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Post> searchApprovedOfficialPosts(@Param("query") String query, Pageable pageable);

    @Query(value = "SELECT p FROM Post p " +
                   "LEFT JOIN FETCH p.department " +
                   "LEFT JOIN FETCH p.author a " +
                   "LEFT JOIN FETCH a.role " +
                   "LEFT JOIN FETCH a.department " +
                   "WHERE p.status = 'PENDING_APPROVAL' AND p.isDeleted = false " +
                   "ORDER BY p.createdAt ASC",
           countQuery = "SELECT count(p) FROM Post p WHERE p.status = 'PENDING_APPROVAL' AND p.isDeleted = false")
    Page<Post> findPendingModerationPosts(Pageable pageable);

    @Query(value = "SELECT p FROM Post p " +
                   "LEFT JOIN FETCH p.department " +
                   "LEFT JOIN FETCH p.author a " +
                   "LEFT JOIN FETCH a.role " +
                   "LEFT JOIN FETCH a.department " +
                   "WHERE p.postType = 'STUDENT_FORUM' AND p.status = 'APPROVED' AND p.isDeleted = false AND " +
                   "(:deptId IS NULL OR p.department.id = :deptId) AND " +
                   "(:query IS NULL OR :query = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "ORDER BY p.createdAt DESC",
           countQuery = "SELECT count(p) FROM Post p WHERE p.postType = 'STUDENT_FORUM' AND p.status = 'APPROVED' AND p.isDeleted = false AND " +
                        "(:deptId IS NULL OR p.department.id = :deptId) AND " +
                        "(:query IS NULL OR :query = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Post> searchApprovedForumPosts(@Param("query") String query, @Param("deptId") Long deptId, Pageable pageable);

    @Query(value = "SELECT p FROM Post p " +
                   "LEFT JOIN FETCH p.department " +
                   "LEFT JOIN FETCH p.author a " +
                   "LEFT JOIN FETCH a.role " +
                   "LEFT JOIN FETCH a.department " +
                   "WHERE p.author.id = :authorId AND p.isDeleted = false " +
                   "ORDER BY p.createdAt DESC",
           countQuery = "SELECT count(p) FROM Post p WHERE p.author.id = :authorId AND p.isDeleted = false")
    Page<Post> findPostsByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    long countByStatusAndIsDeletedFalse(String status);

    boolean existsByTitle(String title);

    Optional<Post> findFirstByTitleContainingAndIsDeletedFalse(String title);
}
