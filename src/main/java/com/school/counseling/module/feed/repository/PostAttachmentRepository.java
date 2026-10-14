package com.school.counseling.module.feed.repository;

import com.school.counseling.module.feed.entity.PostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

    List<PostAttachment> findByPostIdAndIsDeletedFalse(Long postId);
}
