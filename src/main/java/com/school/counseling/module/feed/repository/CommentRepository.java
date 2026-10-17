package com.school.counseling.module.feed.repository;

import com.school.counseling.module.feed.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
           "LEFT JOIN FETCH c.author a " +
           "LEFT JOIN FETCH a.role " +
           "LEFT JOIN FETCH a.department " +
           "LEFT JOIN FETCH c.parent " +
           "WHERE c.post.id = :postId AND c.isDeleted = false " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);

    long countByPostIdAndIsDeletedFalse(Long postId);
}
