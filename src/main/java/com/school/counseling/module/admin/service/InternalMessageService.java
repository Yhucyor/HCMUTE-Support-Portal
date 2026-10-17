package com.school.counseling.module.admin.service;

import com.school.counseling.common.exception.ResourceNotFoundException;
import com.school.counseling.module.admin.dto.InternalMessageDto;
import com.school.counseling.module.admin.dto.SendInternalMessageRequest;
import com.school.counseling.module.admin.entity.InternalStaffAdminMessage;
import com.school.counseling.module.admin.repository.InternalStaffAdminMessageRepository;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalMessageService {

    private final InternalStaffAdminMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public InternalMessageDto sendMessage(Long senderId, SendInternalMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", senderId));

        User receiver = null;
        if (request.getReceiverId() != null) {
            receiver = userRepository.findById(request.getReceiverId()).orElse(null);
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        } else if (sender.getDepartment() != null) {
            department = sender.getDepartment();
        }

        InternalStaffAdminMessage message = InternalStaffAdminMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .department(department)
                .subject(request.getSubject() != null ? request.getSubject().trim() : "Trao đổi nội bộ")
                .content(request.getContent().trim())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "INQUIRY")
                .isRead(false)
                .build();

        message = messageRepository.save(message);
        log.info("Gửi tin nhắn nội bộ thành công: ID={}, Sender={}, Dept={}", message.getId(), sender.getFullName(), department != null ? department.getName() : "All");

        return mapToDto(message);
    }

    public List<InternalMessageDto> getMessages(Long departmentId) {
        List<InternalStaffAdminMessage> list;
        if (departmentId != null) {
            list = messageRepository.findRecentMessagesByDepartment(departmentId);
        } else {
            list = messageRepository.findAllRecentMessages();
        }
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long messageId) {
        InternalStaffAdminMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn nội bộ", "id", messageId));
        message.setIsRead(true);
        messageRepository.save(message);
    }

    private InternalMessageDto mapToDto(InternalStaffAdminMessage m) {
        return InternalMessageDto.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFullName())
                .senderRole(m.getSender().getRole() != null ? m.getSender().getRole().getName() : null)
                .senderDepartmentName(m.getSender().getDepartment() != null ? m.getSender().getDepartment().getName() : null)
                .receiverId(m.getReceiver() != null ? m.getReceiver().getId() : null)
                .receiverName(m.getReceiver() != null ? m.getReceiver().getFullName() : null)
                .departmentId(m.getDepartment() != null ? m.getDepartment().getId() : null)
                .departmentName(m.getDepartment() != null ? m.getDepartment().getName() : "Toàn trường / Ban Quản Trị")
                .subject(m.getSubject())
                .content(m.getContent())
                .messageType(m.getMessageType())
                .isRead(m.getIsRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
