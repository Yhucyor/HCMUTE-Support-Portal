/**
 * Module quản lý bộ lọc, phân trang và Accordion FAQ - Tuân thủ CSP Level 3.
 * Áp dụng Event Delegation toàn diện, Phân trang Client-side và Tìm kiếm Real-time linh động.
 */
document.addEventListener('DOMContentLoaded', () => {
    let activeCategory = 'all';
    let activeDept = 'all';
    let activeTab = 'all';
    let searchQuery = '';
    let currentPage = 1;
    const pageSize = 10; // Mỗi trang hiển thị 10 câu hỏi để tránh tràn màn hình

    const faqList = document.getElementById('faq-list');
    const searchForm = document.getElementById('faq-search-form');
    const searchInput = document.getElementById('faq-search-input');
    const searchClearBtn = document.getElementById('search-clear-btn');
    const searchSubmitBtn = document.getElementById('search-submit-btn');
    const counterBadge = document.getElementById('faq-counter-badge');
    const emptyState = document.getElementById('faq-empty-state');
    const deptFilter = document.getElementById('dept-filter');
    const faqTabs = document.querySelectorAll('#faq-tabs .faq-tab');
    const searchChips = document.querySelectorAll('.search-chip');
    const categoryContainer = document.getElementById('category-grid');
    const paginationContainer = document.getElementById('faq-pagination-container');
    const paginationControls = document.getElementById('pagination-controls');
    const paginationInfo = document.getElementById('pagination-info');
    const paginationTotal = document.getElementById('pagination-total');

    // Đọc query từ URL hoặc input có sẵn nếu có
    if (searchInput && searchInput.value.trim().length > 0) {
        searchQuery = searchInput.value.trim().toLowerCase();
        if (searchClearBtn) searchClearBtn.classList.remove('hidden');
    }

    // 1. Áp dụng bộ lọc và phân trang
    function applyFilters() {
        if (!faqList) return;
        const faqItems = Array.from(faqList.querySelectorAll('.faq-item'));
        
        // Cập nhật số đếm động trên các pills danh mục dựa trên từ khóa & phòng ban hiện tại
        updateCategoryPillCounts(faqItems);

        // Lọc các item thỏa điều kiện
        const matchingItems = faqItems.filter(item => {
            const itemCat = item.getAttribute('data-cat') || '';
            const itemDept = item.getAttribute('data-dept') || '';
            const itemType = item.getAttribute('data-type') || '';
            const textContent = item.textContent.toLowerCase();

            const matchCat = (activeCategory === 'all' || itemCat === activeCategory);
            const matchDept = (activeDept === 'all' || itemDept === activeDept);
            const matchTab = (activeTab === 'all' || itemType === activeTab || itemType === 'all');
            const matchSearch = (!searchQuery || textContent.includes(searchQuery));

            return matchCat && matchDept && matchTab && matchSearch;
        });

        // Sắp xếp: Mặc định ưu tiên năm 2026, 2025 (timestamp giảm dần) lên đầu
        if (activeTab === 'popular') {
            matchingItems.sort((a, b) => {
                const viewsA = parseInt(a.getAttribute('data-views'), 10) || 0;
                const viewsB = parseInt(b.getAttribute('data-views'), 10) || 0;
                return viewsB - viewsA;
            });
        } else {
            matchingItems.sort((a, b) => {
                const tsA = parseInt(a.getAttribute('data-timestamp'), 10) || 0;
                const tsB = parseInt(b.getAttribute('data-timestamp'), 10) || 0;
                return tsB - tsA;
            });
        }

        const totalItems = matchingItems.length;
        const totalPages = Math.ceil(totalItems / pageSize) || 1;

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        const startIndex = (currentPage - 1) * pageSize;
        const endIndex = startIndex + pageSize;

        // Ẩn toàn bộ items trước
        faqItems.forEach(item => {
            item.style.display = 'none';
        });

        // Chỉ hiển thị các items thuộc trang hiện tại theo đúng thứ tự đã sắp xếp
        matchingItems.forEach((item, index) => {
            if (index >= startIndex && index < endIndex) {
                faqList.appendChild(item);
                item.style.display = 'block';
            }
        });

        // Cập nhật số đếm trên Toolbar
        if (counterBadge) {
            if (searchQuery) {
                counterBadge.textContent = `${totalItems} KẾT QUẢ`;
            } else {
                counterBadge.textContent = `${totalItems} CÂU HỎI`;
            }
        }

        // Toggle Empty state
        if (emptyState) {
            emptyState.classList.toggle('hidden', totalItems > 0);
        }

        // Render Pagination Controls
        renderPagination(totalItems, totalPages, startIndex, endIndex);
    }

    // Cập nhật linh động số lượng hiển thị trên từng badge danh mục theo thời gian thực
    function updateCategoryPillCounts(faqItems) {
        const counts = {
            'all': 0,
            'hoc-vu': 0,
            'hoc-phi': 0,
            'quy-che': 0,
            'ngoai-ngu': 0
        };

        faqItems.forEach(item => {
            const itemDept = item.getAttribute('data-dept') || '';
            const textContent = item.textContent.toLowerCase();

            const matchDept = (activeDept === 'all' || itemDept === activeDept);
            const matchSearch = (!searchQuery || textContent.includes(searchQuery));

            if (matchDept && matchSearch) {
                counts['all']++;
                const cat = item.getAttribute('data-cat') || '';
                if (counts[cat] !== undefined) {
                    counts[cat]++;
                }
            }
        });

        for (const [k, v] of Object.entries(counts)) {
            const badge = document.getElementById('count-' + k);
            if (badge) {
                badge.textContent = String(v);
            }
        }
    }

    // 2. Render thanh điều hướng phân trang
    function renderPagination(totalItems, totalPages, startIndex, endIndex) {
        if (!paginationContainer || !paginationControls) return;

        if (totalItems <= pageSize) {
            paginationContainer.classList.add('hidden');
            return;
        }

        paginationContainer.classList.remove('hidden');

        // Cập nhật text trạng thái
        if (paginationInfo) {
            const from = totalItems > 0 ? startIndex + 1 : 0;
            const to = Math.min(endIndex, totalItems);
            paginationInfo.textContent = `${from} - ${to}`;
        }
        if (paginationTotal) {
            paginationTotal.textContent = String(totalItems);
        }

        // Tạo các nút điều hướng
        let html = '';

        // Nút Trang trước
        const prevDisabled = currentPage === 1;
        html += `
            <button type="button" class="page-btn-prev px-2.5 py-1.5 rounded-lg border border-[#e2e8f0] text-xs font-semibold flex items-center justify-center transition-all ${prevDisabled ? 'opacity-40 cursor-not-allowed bg-slate-50 text-slate-400' : 'hover:bg-[#eff4ff] hover:text-[#0037b0] text-[#434655] cursor-pointer'}" ${prevDisabled ? 'disabled' : ''}>
                <span class="material-symbols-outlined text-[16px]">chevron_left</span>
            </button>
        `;

        // Tính danh sách các trang cần hiển thị
        let pagesToShow = [];
        if (totalPages <= 7) {
            for (let i = 1; i <= totalPages; i++) pagesToShow.push(i);
        } else {
            pagesToShow.push(1);
            if (currentPage > 3) pagesToShow.push('...');
            const start = Math.max(2, currentPage - 1);
            const end = Math.min(totalPages - 1, currentPage + 1);
            for (let i = start; i <= end; i++) pagesToShow.push(i);
            if (currentPage < totalPages - 2) pagesToShow.push('...');
            pagesToShow.push(totalPages);
        }

        pagesToShow.forEach(p => {
            if (p === '...') {
                html += `<span class="px-2 py-1 text-xs text-slate-400">...</span>`;
            } else {
                const isActive = p === currentPage;
                html += `
                    <button type="button" data-page="${p}" class="page-num-btn w-8 h-8 rounded-lg text-xs font-semibold flex items-center justify-center transition-all ${isActive ? 'bg-[#0037b0] text-white shadow-xs' : 'border border-[#e2e8f0] hover:bg-[#eff4ff] hover:text-[#0037b0] text-[#434655] cursor-pointer'}">
                        ${p}
                    </button>
                `;
            }
        });

        // Nút Trang sau
        const nextDisabled = currentPage === totalPages;
        html += `
            <button type="button" class="page-btn-next px-2.5 py-1.5 rounded-lg border border-[#e2e8f0] text-xs font-semibold flex items-center justify-center transition-all ${nextDisabled ? 'opacity-40 cursor-not-allowed bg-slate-50 text-slate-400' : 'hover:bg-[#eff4ff] hover:text-[#0037b0] text-[#434655] cursor-pointer'}" ${nextDisabled ? 'disabled' : ''}>
                <span class="material-symbols-outlined text-[16px]">chevron_right</span>
            </button>
        `;

        paginationControls.innerHTML = html;
    }

    // 3. Xử lý sự kiện click trên thanh phân trang (Event Delegation)
    if (paginationControls) {
        paginationControls.addEventListener('click', (e) => {
            const pageNumBtn = e.target.closest('.page-num-btn');
            if (pageNumBtn) {
                const targetPage = parseInt(pageNumBtn.getAttribute('data-page'), 10);
                if (targetPage && targetPage !== currentPage) {
                    currentPage = targetPage;
                    applyFilters();
                    scrollToFaqList();
                }
                return;
            }

            const prevBtn = e.target.closest('.page-btn-prev');
            if (prevBtn && !prevBtn.disabled) {
                if (currentPage > 1) {
                    currentPage--;
                    applyFilters();
                    scrollToFaqList();
                }
                return;
            }

            const nextBtn = e.target.closest('.page-btn-next');
            if (nextBtn && !nextBtn.disabled) {
                currentPage++;
                applyFilters();
                scrollToFaqList();
                return;
            }
        });
    }

    function scrollToFaqList() {
        const target = document.getElementById('faq-counter-badge') || faqList;
        if (target) {
            const topPos = target.getBoundingClientRect().top + window.pageYOffset - 90;
            window.scrollTo({ top: Math.max(0, topPos), behavior: 'smooth' });
        }
    }

    // 4. Event Delegation cho Accordion Toggle & Helpful Rating
    if (faqList) {
        faqList.addEventListener('click', (e) => {
            // Xử lý Accordion Toggle
            const toggleBtn = e.target.closest('.faq-toggle');
            if (toggleBtn) {
                const item = toggleBtn.closest('.faq-item');
                if (!item) return;
                const content = item.querySelector('.faq-content');
                const chevron = item.querySelector('.faq-chevron');

                if (content) {
                    const isHidden = content.classList.contains('hidden');
                    if (isHidden) {
                        content.classList.remove('hidden');
                        if (chevron) chevron.classList.add('rotate-180');
                    } else {
                        content.classList.add('hidden');
                        if (chevron) chevron.classList.remove('rotate-180');
                    }
                }
                return;
            }

            // Xử lý Nút Helpful 👍 / 👎
            const helpfulBtn = e.target.closest('.helpful-btn');
            if (helpfulBtn) {
                e.stopPropagation();
                const countSpan = helpfulBtn.querySelector('.count');
                if (countSpan && !helpfulBtn.classList.contains('voted')) {
                    const currentCount = parseInt(countSpan.textContent.replace(/[^\d]/g, ''), 10) || 0;
                    countSpan.textContent = String(currentCount + 1);
                    helpfulBtn.classList.add('bg-[#dce1ff]', 'text-[#0037b0]', 'voted');
                }
            }
        });
    }

    // 5. Event Delegation cho Category Filter Pills
    if (categoryContainer) {
        categoryContainer.addEventListener('click', (e) => {
            const pill = e.target.closest('.category-pill');
            if (!pill) return;

            activeCategory = pill.getAttribute('data-category') || 'all';
            currentPage = 1; // Reset về trang 1 khi đổi danh mục

            // Reset toàn bộ pills
            categoryContainer.querySelectorAll('.category-pill').forEach(p => {
                p.className = 'category-card category-pill px-4 py-2 rounded-full text-xs font-medium bg-white text-[#434655] hover:text-[#0037b0] hover:bg-[#eff4ff] border border-[#c4c5d7]/60 shadow-sm flex items-center gap-1.5 transition-all cursor-pointer';
                const count = p.querySelector('.pill-count');
                if (count) count.className = 'pill-count px-1.5 py-0.5 rounded-full text-[11px] font-bold bg-[#eff4ff] text-[#0037b0]';
            });

            // Active pill được chọn
            pill.className = 'category-card category-pill px-4 py-2 rounded-full text-xs font-semibold bg-[#0037b0] text-white border border-[#0037b0] shadow-sm flex items-center gap-1.5 transition-all cursor-pointer';
            const activeCount = pill.querySelector('.pill-count');
            if (activeCount) activeCount.className = 'pill-count px-1.5 py-0.5 rounded-full text-[11px] font-bold bg-white/20 text-white';

            applyFilters();
        });
    }

    // 6. Tabs (Tất cả, Phổ biến, Mới cập nhật)
    faqTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            faqTabs.forEach(t => {
                t.className = 'faq-tab px-3 py-1 rounded-lg text-xs text-[#565e74] hover:text-[#0b1c30] transition-all cursor-pointer';
            });
            tab.className = 'faq-tab px-3 py-1 rounded-lg text-xs bg-white text-[#0037b0] font-semibold shadow-xs transition-all cursor-pointer';
            activeTab = tab.getAttribute('data-tab') || 'all';
            currentPage = 1;
            applyFilters();
        });
    });

    // 7. Chặn submit reload trang & Tìm kiếm từ khóa Live linh động
    if (searchForm) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault(); // Tuyệt đối không reload trang!
            if (searchInput) {
                searchQuery = searchInput.value.trim().toLowerCase();
            }
            currentPage = 1;
            applyFilters();
            scrollToFaqList();
        });
    }

    if (searchSubmitBtn) {
        searchSubmitBtn.addEventListener('click', (e) => {
            e.preventDefault(); // Tuyệt đối không reload trang!
            if (searchInput) {
                searchQuery = searchInput.value.trim().toLowerCase();
            }
            currentPage = 1;
            applyFilters();
            scrollToFaqList();
        });
    }

    if (searchInput) {
        // Gõ phím tới đâu, danh sách chạy theo thời gian thực tới đó
        searchInput.addEventListener('input', (e) => {
            searchQuery = e.target.value.trim().toLowerCase();
            currentPage = 1;
            if (searchClearBtn) searchClearBtn.classList.toggle('hidden', searchQuery.length === 0);
            applyFilters();
        });

        // Bắt phím Enter trên bàn phím máy tính để tìm kiếm và cuộn mượt xuống kết quả
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                searchQuery = searchInput.value.trim().toLowerCase();
                currentPage = 1;
                applyFilters();
                scrollToFaqList();
            }
        });
    }

    if (searchClearBtn) {
        searchClearBtn.addEventListener('click', () => {
            if (searchInput) {
                searchInput.value = '';
                searchInput.focus();
            }
            searchQuery = '';
            currentPage = 1;
            searchClearBtn.classList.add('hidden');
            applyFilters();
        });
    }

    // 8. Chips gợi ý tìm kiếm nhanh
    searchChips.forEach(chip => {
        chip.addEventListener('click', () => {
            if (searchInput) {
                searchInput.value = chip.textContent.trim();
                searchQuery = searchInput.value.toLowerCase();
            }
            currentPage = 1;
            if (searchClearBtn) searchClearBtn.classList.remove('hidden');
            applyFilters();
        });
    });

    // 9. Đơn vị phòng ban dropdown
    if (deptFilter) {
        deptFilter.addEventListener('change', (e) => {
            activeDept = e.target.value;
            currentPage = 1;
            applyFilters();
        });
    }

    // Khởi chạy phân trang và bộ lọc ban đầu
    applyFilters();
});
