/**
 * User Account Dropdown Menu Handler
 * Requirements: 2, 9, 11 - Toggle, Close, Keyboard Navigation, Mobile Support
 */
(function() {
    'use strict';
    
    const userAccountToggle = document.getElementById('userAccountToggle');
    const dropdownMenu = document.getElementById('userDropdownMenu');
    
    if (!userAccountToggle || !dropdownMenu) {
        return; // Exit if elements don't exist
    }
    
    let isOpen = false;
    let focusedItemIndex = -1;
    const menuItems = dropdownMenu.querySelectorAll('.dropdown-item');
    
    /**
     * Toggle dropdown menu
     */
    function toggleDropdown(event) {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }
        
        isOpen = !isOpen;
        
        if (isOpen) {
            openDropdown();
        } else {
            closeDropdown();
        }
    }
    
    /**
     * Open dropdown with animation (Requirement 2)
     */
    function openDropdown() {
        dropdownMenu.classList.add('show');
        userAccountToggle.setAttribute('aria-expanded', 'true');
        isOpen = true;
        focusedItemIndex = -1;
        
        // Set tabindex for keyboard navigation
        menuItems.forEach(item => item.setAttribute('tabindex', '0'));
    }
    
    /**
     * Close dropdown (Requirement 2)
     */
    function closeDropdown() {
        dropdownMenu.classList.remove('show');
        userAccountToggle.setAttribute('aria-expanded', 'false');
        isOpen = false;
        focusedItemIndex = -1;
        
        // Remove tabindex when closed
        menuItems.forEach(item => item.setAttribute('tabindex', '-1'));
    }
    
    /**
     * Focus menu item by index (Requirement 11)
     */
    function focusMenuItem(index) {
        if (index < 0 || index >= menuItems.length) {
            return;
        }
        
        // Remove focus from all items
        menuItems.forEach(item => item.blur());
        
        // Focus target item
        menuItems[index].focus();
        focusedItemIndex = index;
    }
    
    /**
     * Handle keyboard navigation (Requirement 11)
     */
    function handleKeydown(event) {
        // Toggle on Enter or Space when focused on toggle button
        if (event.target === userAccountToggle) {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                toggleDropdown();
                
                // Focus first item when opening
                if (isOpen && menuItems.length > 0) {
                    setTimeout(() => focusMenuItem(0), 50);
                }
            }
            return;
        }
        
        // Close on Escape (Requirement 2)
        if (event.key === 'Escape' && isOpen) {
            event.preventDefault();
            closeDropdown();
            userAccountToggle.focus();
            return;
        }
        
        // Navigate with Tab/Shift+Tab (Requirement 11)
        if (event.key === 'Tab' && isOpen) {
            event.preventDefault();
            
            if (event.shiftKey) {
                // Shift+Tab: Previous item
                const prevIndex = focusedItemIndex <= 0 ? menuItems.length - 1 : focusedItemIndex - 1;
                focusMenuItem(prevIndex);
            } else {
                // Tab: Next item
                const nextIndex = focusedItemIndex >= menuItems.length - 1 ? 0 : focusedItemIndex + 1;
                focusMenuItem(nextIndex);
            }
        }
        
        // Navigate with Arrow keys
        if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && isOpen) {
            event.preventDefault();
            
            if (event.key === 'ArrowDown') {
                const nextIndex = focusedItemIndex >= menuItems.length - 1 ? 0 : focusedItemIndex + 1;
                focusMenuItem(nextIndex);
            } else {
                const prevIndex = focusedItemIndex <= 0 ? menuItems.length - 1 : focusedItemIndex - 1;
                focusMenuItem(prevIndex);
            }
        }
        
        // Activate on Enter when item is focused (Requirement 11)
        if (event.key === 'Enter' && event.target.classList.contains('dropdown-item')) {
            event.target.click();
        }
    }
    
    /**
     * Handle click outside to close (Requirement 2)
     */
    function handleClickOutside(event) {
        if (!isOpen) return;
        
        const isClickInside = userAccountToggle.contains(event.target) || 
                             dropdownMenu.contains(event.target);
        
        if (!isClickInside) {
            closeDropdown();
        }
    }
    
    /**
     * Handle scroll on mobile to close (Requirement 9)
     */
    let scrollTimeout;
    let lastScrollY = window.scrollY;
    
    function handleScroll() {
        if (!isOpen) return;
        
        const isMobile = window.innerWidth < 768;
        if (!isMobile) return;
        
        const currentScrollY = window.scrollY;
        const scrollDiff = Math.abs(currentScrollY - lastScrollY);
        
        // Close if scrolled more than 10px (Requirement 9)
        if (scrollDiff > 10) {
            closeDropdown();
        }
        
        lastScrollY = currentScrollY;
    }
    
    /**
     * Track focused item index on focus events
     */
    menuItems.forEach((item, index) => {
        item.addEventListener('focus', () => {
            focusedItemIndex = index;
        });
    });
    
    // Event listeners
    userAccountToggle.addEventListener('click', toggleDropdown);
    userAccountToggle.addEventListener('keydown', handleKeydown);
    document.addEventListener('click', handleClickOutside);
    document.addEventListener('keydown', handleKeydown);
    window.addEventListener('scroll', handleScroll, { passive: true });
    
    // Cleanup on page unload
    window.addEventListener('beforeunload', () => {
        document.removeEventListener('click', handleClickOutside);
        document.removeEventListener('keydown', handleKeydown);
        window.removeEventListener('scroll', handleScroll);
    });
    
})();
