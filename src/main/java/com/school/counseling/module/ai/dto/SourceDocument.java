package com.school.counseling.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho mỗi nguồn trích dẫn từ AI RAG
 * Bao gồm text snippet, metadata, score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocument {
    
    private String text; // Đoạn văn bản trích dẫn (200-500 chars)
    
    private String source; // Tên văn bản gốc (VD: "Quyết định 123/QĐ-ĐHSPKT")
    
    private String url; // Link tới PDF/Word gốc (nếu có)
    
    private Double score; // Relevance score (0.0 - 1.0)
    
    @JsonProperty("page_number")
    private Integer pageNumber; // Số trang trong tài liệu
    
    @JsonProperty("document_type")
    private String documentType; // "quy_che", "thong_bao", "de_an", "faq"
    
    private String department; // Phòng/Khoa quản lý văn bản
    
    @JsonProperty("published_date")
    private String publishedDate; // Ngày ban hành (ISO format)
}
