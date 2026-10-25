/**
 * FORUM INTERACTIONS MODULE (HCMUTE Portal)
 * Tối ưu hóa trải nghiệm Diễn đàn Sinh viên:
 * 1. Tích hợp Quill.js Snow offline cho Modal đăng bài tinh gọn
 * 2. Tính năng Xem trước bài viết (Live Preview) ở góc dưới bên trái
 * 3. Thả tim (Like) tức thì qua REST API (Optimistic UI - Zero Reload)
 * 4. Mở xem danh sách bình luận (Comment Accordion) và gửi bình luận realtime
 */

document.addEventListener('DOMContentLoaded', function () {
    let quill = null;
    let isPreviewMode = false;

    // ==========================================
    // 1. KHỞI TẠO QUILL.JS CHO MODAL ĐĂNG BÀI
    // ==========================================
    function initQuillEditor() {
        const editorContainer = document.getElementById('quillEditor');
        if (!editorContainer || quill) return;

        if (typeof Quill === 'undefined') {
            console.error('Quill.js chưa được tải thành công từ vendor cục bộ.');
            return;
        }

        quill = new Quill('#quillEditor', {
            theme: 'snow',
            placeholder: 'Bạn muốn chia sẻ hay thảo luận điều gì với sinh viên HCMUTE?...',
            modules: {
                toolbar: [
                    ['bold', 'italic', 'underline', 'strike'],
                    ['blockquote', 'code-block'],
                    [{ 'list': 'ordered' }, { 'list': 'bullet' }],
                    ['link', 'clean']
                ]
            }
        });
    }

    // ==========================================
    // 2. MODAL CONTROLS (MỞ / ĐÓNG MODAL NHANH)
    // ==========================================
    const modal = document.getElementById('createPostModal');
    const openBtns = document.querySelectorAll('.trigger-open-create-modal');
    const closeBtns = document.querySelectorAll('.trigger-close-create-modal');

    window.openCreateModal = function () {
        if (!modal) return;
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        document.body.classList.add('overflow-hidden');
        
        // Khởi tạo Quill ngay khi mở modal
        initQuillEditor();
        
        // Đặt focus vào ô tiêu đề
        const titleInput = document.getElementById('modalPostTitle');
        if (titleInput) {
            setTimeout(() => titleInput.focus(), 100);
        }
    };

    window.closeCreateModal = function () {
        if (!modal) return;
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        document.body.classList.remove('overflow-hidden');

        // Reset chế độ xem trước về chế độ soạn thảo
        if (isPreviewMode) {
            togglePreview();
        }
    };

    openBtns.forEach(btn => btn.addEventListener('click', (e) => {
        e.preventDefault();
        openCreateModal();
    }));

    closeBtns.forEach(btn => btn.addEventListener('click', (e) => {
        e.preventDefault();
        closeCreateModal();
    }));

    // Đóng khi click ngoài backdrop
    if (modal) {
        modal.addEventListener('click', function (e) {
            if (e.target === modal) {
                closeCreateModal();
            }
        });
    }

    // Đóng khi nhấn phím Escape
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && modal && !modal.classList.contains('hidden')) {
            closeCreateModal();
        }
    });

    // ==========================================
    // 3. ĐẾM KÝ TỰ TIÊU ĐỀ
    // ==========================================
    const titleInput = document.getElementById('modalPostTitle');
    const titleCounter = document.getElementById('modalTitleCounter');
    if (titleInput && titleCounter) {
        titleInput.addEventListener('input', function () {
            const len = this.value.length;
            titleCounter.textContent = `${len}/150`;
            if (len >= 140) {
                titleCounter.classList.add('text-amber-600', 'font-bold');
            } else {
                titleCounter.classList.remove('text-amber-600', 'font-bold');
            }
        });
    }

    // ==========================================
    // 4. XỬ LÝ ĐÍNH KÈM & XEM TRƯỚC HÌNH ẢNH
    // ==========================================
    const imageInput = document.getElementById('modalPostImage');
    const imagePreviewBox = document.getElementById('modalImagePreviewBox');
    const imagePreviewImg = document.getElementById('modalImagePreviewImg');
    const removeImageBtn = document.getElementById('modalRemoveImageBtn');
    let selectedImageSrc = null;

    if (imageInput) {
        imageInput.addEventListener('change', function () {
            if (this.files && this.files[0]) {
                const file = this.files[0];
                if (file.size > 10 * 1024 * 1024) {
                    alert('Kích thước ảnh không được vượt quá 10MB.');
                    this.value = '';
                    return;
                }
                const reader = new FileReader();
                reader.onload = function (e) {
                    selectedImageSrc = e.target.result;
                    if (imagePreviewImg) imagePreviewImg.src = selectedImageSrc;
                    if (imagePreviewBox) imagePreviewBox.classList.remove('hidden');
                };
                reader.readAsDataURL(file);
            }
        });
    }

    if (removeImageBtn) {
        removeImageBtn.addEventListener('click', function () {
            if (imageInput) imageInput.value = '';
            selectedImageSrc = null;
            if (imagePreviewBox) imagePreviewBox.classList.add('hidden');
            if (imagePreviewImg) imagePreviewImg.src = '';
        });
    }

    // ==========================================
    // 5. TÍNH NĂNG "XEM TRƯỚC ĐỊNH DẠNG BÀI VIẾT" (LIVE PREVIEW)
    // ==========================================
    const btnTogglePreview = document.getElementById('btnTogglePreview');
    const previewBtnText = document.getElementById('previewBtnText');
    const previewBtnIcon = document.getElementById('previewBtnIcon');
    const editorSection = document.getElementById('modalEditorSection');
    const previewSection = document.getElementById('modalPreviewSection');

    function togglePreview() {
        if (!editorSection || !previewSection) return;

        isPreviewMode = !isPreviewMode;

        if (isPreviewMode) {
            // Lấy dữ liệu hiện tại
            const title = (titleInput && titleInput.value.trim()) ? titleInput.value.trim() : 'Tiêu đề bài viết xem trước';
            const deptSelect = document.getElementById('modalPostDept');
            const deptName = (deptSelect && deptSelect.selectedIndex >= 0 && deptSelect.value) 
                ? deptSelect.options[deptSelect.selectedIndex].text 
                : 'Toàn trường';

            const rawHtml = quill ? quill.root.innerHTML : '';
            const previewTitle = document.getElementById('previewCardTitle');
            const previewDept = document.getElementById('previewCardDept');
            const previewContent = document.getElementById('previewCardContent');
            const previewImageBox = document.getElementById('previewCardImageBox');
            const previewImage = document.getElementById('previewCardImage');

            if (previewTitle) previewTitle.textContent = title;
            if (previewDept) previewDept.textContent = deptName;
            if (previewContent) {
                previewContent.innerHTML = (rawHtml && rawHtml !== '<p><br></p>') 
                    ? rawHtml 
                    : '<p class="text-slate-400 italic">Chưa có nội dung bài viết...</p>';
            }

            if (selectedImageSrc && previewImageBox && previewImage) {
                previewImage.src = selectedImageSrc;
                previewImageBox.classList.remove('hidden');
            } else if (previewImageBox) {
                previewImageBox.classList.add('hidden');
            }

            // Chuyển sang màn hình xem trước
            editorSection.classList.add('hidden');
            previewSection.classList.remove('hidden');
            if (previewBtnText) previewBtnText.textContent = 'Quay lại chỉnh sửa';
            if (previewBtnIcon) previewBtnIcon.textContent = 'edit_note';
        } else {
            // Quay lại màn hình soạn thảo
            previewSection.classList.add('hidden');
            editorSection.classList.remove('hidden');
            if (previewBtnText) previewBtnText.textContent = 'Xem trước bài viết';
            if (previewBtnIcon) previewBtnIcon.textContent = 'visibility';
        }
    }

    if (btnTogglePreview) {
        btnTogglePreview.addEventListener('click', togglePreview);
    }

    // ==========================================
    // 6. SUBMIT FORM ĐĂNG BÀI
    // ==========================================
    const createPostForm = document.getElementById('createPostForm');
    const hiddenContentInput = document.getElementById('hiddenPostContent');

    if (createPostForm) {
        createPostForm.addEventListener('submit', function (e) {
            if (!quill) {
                e.preventDefault();
                alert('Trình soạn thảo chưa sẵn sàng. Vui lòng thử lại sau giây lát.');
                return;
            }

            const htmlContent = quill.root.innerHTML;
            const plainText = quill.getText().trim();

            if (!plainText && htmlContent === '<p><br></p>') {
                e.preventDefault();
                alert('Vui lòng nhập nội dung bài viết thảo luận.');
                if (isPreviewMode) togglePreview();
                quill.focus();
                return;
            }

            if (hiddenContentInput) {
                hiddenContentInput.value = htmlContent;
            }
        });
    }

    // ==========================================
    // 7. THẢ TIM (LIKE) TỨC THÌ QUA REST API (OPTIMISTIC UI)
    // ==========================================
    document.addEventListener('click', async function (e) {
        const likeBtn = e.target.closest('.btn-like-post');
        if (!likeBtn) return;

        e.preventDefault();
        const postId = likeBtn.dataset.postId;
        if (!postId) return;

        const heartIcon = likeBtn.querySelector('.like-heart-icon');
        const countSpan = likeBtn.querySelector('.like-count-text');

        // Đọc trạng thái hiện tại
        const wasLiked = likeBtn.dataset.liked === 'true';
        let count = parseInt(likeBtn.dataset.likeCount || '0', 10);

        // Optimistic UI update: cập nhật ngay tức thì
        const newLiked = !wasLiked;
        const newCount = newLiked ? count + 1 : Math.max(0, count - 1);

        likeBtn.dataset.liked = newLiked ? 'true' : 'false';
        likeBtn.dataset.likeCount = newCount;

        if (countSpan) countSpan.textContent = `${newCount} Thích`;

        if (newLiked) {
            likeBtn.classList.remove('text-slate-600');
            likeBtn.classList.add('text-rose-600', 'font-bold');
            if (heartIcon) {
                heartIcon.style.fontVariationSettings = "'FILL' 1";
                heartIcon.classList.add('scale-125');
                setTimeout(() => heartIcon.classList.remove('scale-125'), 200);
            }
        } else {
            likeBtn.classList.remove('text-rose-600', 'font-bold');
            likeBtn.classList.add('text-slate-600');
            if (heartIcon) {
                heartIcon.style.fontVariationSettings = "'FILL' 0";
            }
        }

        try {
            const resp = await fetch(`/api/v1/forum/posts/${postId}/like`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (resp.status === 401 || resp.status === 403) {
                // Người dùng chưa đăng nhập
                window.location.href = '/auth/login';
                return;
            }

            const data = await resp.json();
            if (data && data.data) {
                // Đồng bộ lại số like chính xác từ máy chủ
                likeBtn.dataset.liked = data.data.liked ? 'true' : 'false';
                likeBtn.dataset.likeCount = data.data.likeCount;
                if (countSpan) countSpan.textContent = `${data.data.likeCount} Thích`;
            }
        } catch (err) {
            console.error('Lỗi khi thả tim bài viết:', err);
            // Revert nếu lỗi mạng
            likeBtn.dataset.liked = wasLiked ? 'true' : 'false';
            likeBtn.dataset.likeCount = count;
            if (countSpan) countSpan.textContent = `${count} Thích`;
            if (wasLiked) {
                likeBtn.classList.add('text-rose-600', 'font-bold');
                likeBtn.classList.remove('text-slate-600');
                if (heartIcon) heartIcon.style.fontVariationSettings = "'FILL' 1";
            } else {
                likeBtn.classList.remove('text-rose-600', 'font-bold');
                likeBtn.classList.add('text-slate-600');
                if (heartIcon) heartIcon.style.fontVariationSettings = "'FILL' 0";
            }
        }
    });

    // ==========================================
    // 8. ACCORDION MỞ XEM BÌNH LUẬN (COMMENT DRAWER)
    // ==========================================
    document.addEventListener('click', function (e) {
        const commentToggleBtn = e.target.closest('.btn-toggle-comments');
        if (!commentToggleBtn) return;

        e.preventDefault();
        const postId = commentToggleBtn.dataset.postId;
        if (!postId) return;

        const accordion = document.getElementById(`comments-accordion-${postId}`);
        if (!accordion) return;

        accordion.classList.toggle('hidden');

        // Cuộn nhẹ và focus vào input bình luận nếu vừa mở
        if (!accordion.classList.contains('hidden')) {
            const input = accordion.querySelector('.comment-input-field');
            if (input) {
                setTimeout(() => input.focus(), 150);
            }
        }
    });

    // ==========================================
    // 9. GỬI BÌNH LUẬN MỚI QUA FETCH API (KHÔNG RELOAD TRANG)
    // ==========================================
    document.addEventListener('submit', async function (e) {
        const commentForm = e.target.closest('.form-comment-ajax');
        if (!commentForm) return;

        e.preventDefault();
        const postId = commentForm.dataset.postId;
        const inputField = commentForm.querySelector('.comment-input-field');
        const submitBtn = commentForm.querySelector('button[type="submit"]');

        if (!postId || !inputField) return;

        const content = inputField.value.trim();
        if (!content) return;

        // Trạng thái gửi
        inputField.disabled = true;
        if (submitBtn) submitBtn.disabled = true;

        try {
            const resp = await fetch(`/api/v1/forum/posts/${postId}/comments`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({
                    content: content,
                    parentId: null
                })
            });

            if (resp.status === 401 || resp.status === 403) {
                window.location.href = '/auth/login';
                return;
            }

            const resData = await resp.json();
            if (resData && resData.data) {
                const cmt = resData.data;

                // Thêm bình luận mới vào danh sách
                const commentsList = document.getElementById(`comments-list-${postId}`);
                if (commentsList) {
                    const emptyState = commentsList.querySelector('.no-comments-placeholder');
                    if (emptyState) emptyState.remove();

                    const newCommentHtml = createCommentHtmlElement(cmt);
                    commentsList.insertAdjacentHTML('beforeend', newCommentHtml);
                }

                // Cập nhật số đếm bình luận
                const commentCountSpans = document.querySelectorAll(`.comment-count-badge-${postId}`);
                commentCountSpans.forEach(span => {
                    const current = parseInt(span.dataset.count || '0', 10);
                    const updated = current + 1;
                    span.dataset.count = updated;
                    span.textContent = `${updated} Thảo luận`;
                });

                // Xóa trắng input
                inputField.value = '';
            } else {
                alert(resData.message || 'Không thể gửi bình luận. Vui lòng thử lại.');
            }
        } catch (err) {
            console.error('Lỗi khi gửi bình luận:', err);
            alert('Lỗi kết nối khi gửi bình luận. Vui lòng kiểm tra lại mạng!');
        } finally {
            inputField.disabled = false;
            if (submitBtn) submitBtn.disabled = false;
            inputField.focus();
        }
    });

    // Helper tạo HTML cho comment mới chèn vào DOM
    function createCommentHtmlElement(cmt) {
        const avatar = cmt.authorAvatar || 'https://ui-avatars.com/api/?name=' + encodeURIComponent(cmt.authorName || 'SV') + '&background=003882&color=fff';
        const roleName = cmt.authorRole || 'STUDENT';
        const isStaff = roleName === 'STAFF' || roleName === 'ADMIN' || roleName === 'TEACHER';
        const badgeClass = isStaff ? 'bg-blue-100 text-[#003882] font-bold' : 'bg-slate-100 text-slate-600 font-medium';
        const roleLabel = isStaff ? (cmt.authorDepartment || 'Cán bộ HCMUTE') : (cmt.authorDepartment || 'Khoa Sinh viên');

        return `
            <div class="flex items-start gap-2.5 p-2 rounded-xl hover:bg-slate-50 transition-colors">
                <img class="w-8 h-8 rounded-full object-cover shadow-2xs border border-slate-200 shrink-0" 
                     src="${avatar}" alt="Avatar">
                <div class="flex-1 bg-slate-100/70 rounded-2xl p-2.5 text-xs">
                    <div class="flex items-center gap-1.5 flex-wrap mb-1">
                        <span class="font-bold text-slate-900">${cmt.authorName || 'Sinh viên'}</span>
                        <span class="px-2 py-0.2 rounded-full text-[10px] ${badgeClass}">${roleLabel}</span>
                        <span class="text-[10px] text-slate-400">• Vừa xong</span>
                    </div>
                    <p class="text-slate-800 leading-relaxed whitespace-pre-line">${escapeHtml(cmt.content)}</p>
                </div>
            </div>
        `;
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, function (m) { return map[m]; });
    }
});
