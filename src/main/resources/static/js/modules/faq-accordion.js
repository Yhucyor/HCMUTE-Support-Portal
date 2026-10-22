/**
 * Module quản lý bộ lọc và Accordion FAQ - Tuân thủ CSP (Không sử dụng inline handlers).
 * Áp dụng Event Delegation toàn diện.
 */
document.addEventListener('DOMContentLoaded', () => {
    let activeCategory = 'all';
    let activeDept = 'all';
    let activeTab = 'all';
    let searchQuery = '';

    const faqList = document.getElementById('faq-list');
    const searchInput = document.getElementById('faq-search-input');
    const searchClearBtn = document.getElementById('search-clear-btn');
    const counterBadge = document.getElementById('faq-counter-badge');
    const emptyState = document.getElementById('faq-empty-state');
    const deptFilter = document.getElementById('dept-filter');
    const faqTabs = document.querySelectorAll('#faq-tabs .faq-tab');
    const searchChips = document.querySelectorAll('.search-chip');

    // 1. Áp dụng bộ lọc đa tiêu chí
    function applyFilters() {
        if (!faqList) return;
        const faqItems = faqList.querySelectorAll('.faq-item');
        let visibleCount = 0;

        faqItems.forEach(item => {
            const itemCat = item.getAttribute('data-cat') || '';
            const itemDept = item.getAttribute('data-dept') || '';
            const itemType = item.getAttribute('data-type') || '';
            const textContent = item.textContent.toLowerCase();

            const matchCat = (activeCategory === 'all' || itemCat === activeCategory);
            const matchDept = (activeDept === 'all' || itemDept === activeDept);
            const matchTab = (activeTab === 'all' || itemType === activeTab || itemType === 'all');
            const matchSearch = (!searchQuery || textContent.includes(searchQuery));

            if (matchCat && matchDept && matchTab && matchSearch) {
                item.style.display = 'block';
                visibleCount++;
            } else {
                item.style.display = 'none';
            }
        });

        if (counterBadge) {
            counterBadge.textContent = `${visibleCount} CÂU HỎI`;
        }

        if (emptyState) {
            emptyState.classList.toggle('hidden', visibleCount > 0);
        }
    }

    // 2. Event Delegation cho Accordion Toggle & Helpful Rating
    if (faqList) {
        faqList.addEventListener('click', (e) => {
            // Xử lý Accordion Toggle
            const toggleBtn = e.target.closest('.faq-toggle');
            if (toggleBtn) {
                const item = toggleBtn.closest('.faq-item');
                const content = item.querySelector('.faq-content');
                const chevron = item.querySelector('.faq-chevron');

                if (content) {
                    const isHidden = content.classList.contains('hidden');
                    content.classList.toggle('hidden');
                    if (chevron) {
                        chevron.classList.toggle('rotate-180', isHidden);
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

    // 3. Event Delegation cho Category Filter Pills
    const categoryContainer = document.getElementById('category-grid');
    if (categoryContainer) {
        categoryContainer.addEventListener('click', (e) => {
            const pill = e.target.closest('.category-pill');
            if (!pill) return;

            activeCategory = pill.getAttribute('data-category') || 'all';

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

    // 4. Tabs (Tất cả, Phổ biến, Mới cập nhật)
    faqTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            faqTabs.forEach(t => {
                t.className = 'faq-tab px-3 py-1 rounded-lg text-xs text-[#565e74] hover:text-[#0b1c30] transition-all cursor-pointer';
            });
            tab.className = 'faq-tab px-3 py-1 rounded-lg text-xs bg-white text-[#0037b0] font-semibold shadow-xs transition-all cursor-pointer';
            activeTab = tab.getAttribute('data-tab') || 'all';
            applyFilters();
        });
    });

    // 5. Tìm kiếm từ khóa Live
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            searchQuery = e.target.value.trim().toLowerCase();
            if (searchClearBtn) searchClearBtn.classList.toggle('hidden', searchQuery.length === 0);
            applyFilters();
        });
    }

    if (searchClearBtn) {
        searchClearBtn.addEventListener('click', () => {
            searchInput.value = '';
            searchQuery = '';
            searchClearBtn.classList.add('hidden');
            applyFilters();
            searchInput.focus();
        });
    }

    // 6. Chips gợi ý tìm kiếm nhanh
    searchChips.forEach(chip => {
        chip.addEventListener('click', () => {
            if (searchInput) {
                searchInput.value = chip.textContent.trim();
                searchQuery = searchInput.value.toLowerCase();
            }
            if (searchClearBtn) searchClearBtn.classList.remove('hidden');
            applyFilters();
        });
    });

    // 7. Đơn vị phòng ban dropdown
    if (deptFilter) {
        deptFilter.addEventListener('change', (e) => {
            activeDept = e.target.value;
            applyFilters();
        });
    }
});
