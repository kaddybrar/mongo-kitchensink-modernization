// API URL
const API_URL = '/api/v1';

// DOM Elements
const memberForm = document.getElementById('memberForm');
const membersTableBody = document.getElementById('membersTableBody');
const noMembersAlert = document.getElementById('noMembers');
const searchInput = document.getElementById('searchInput');
const searchButton = document.getElementById('searchButton');
const clearButton = document.getElementById('clearButton');
const editMemberModal = new bootstrap.Modal(document.getElementById('editMemberModal'));
const editMemberForm = document.getElementById('editMemberForm');
const saveEditButton = document.getElementById('saveEditButton');

// Validation patterns
const EMAIL_PATTERN = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const PHONE_PATTERN = /^\+?[0-9]{10,15}$/;

// Load all members
async function loadMembers() {
    try {
        console.log('Fetching members from:', `${API_URL}/members`);
        const response = await fetch(`${API_URL}/members`);
        console.log('Response status:', response.status);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const members = await response.json();
        console.log('Members loaded:', members);
        
        if (members.length === 0) {
            membersTableBody.innerHTML = '';
            noMembersAlert.classList.remove('d-none');
        } else {
            noMembersAlert.classList.add('d-none');
            renderMembersTable(members);
        }
    } catch (error) {
        console.error('Error loading members:', error);
        showError('Failed to load members. Please try again later.');
    }
}

// Render members table
function renderMembersTable(members) {
    membersTableBody.innerHTML = '';
    
    members.forEach(member => {
        const row = document.createElement('tr');
        
        row.innerHTML = `
            <td>${member.id}</td>
            <td>${member.name}</td>
            <td>${member.email}</td>
            <td>${formatPhoneNumber(member.phoneNumber) || ''}</td>
            <td>
                <button class="btn btn-sm btn-warning btn-action edit-btn" data-id="${member.id}">Edit</button>
                <button class="btn btn-sm btn-danger btn-action delete-btn" data-id="${member.id}">Delete</button>
            </td>
        `;
        
        membersTableBody.appendChild(row);
    });
    
    // Add event listeners to edit and delete buttons
    document.querySelectorAll('.edit-btn').forEach(button => {
        button.addEventListener('click', () => editMember(button.getAttribute('data-id')));
    });
    
    document.querySelectorAll('.delete-btn').forEach(button => {
        button.addEventListener('click', () => deleteMember(button.getAttribute('data-id')));
    });
}

// Format phone number for display
function formatPhoneNumber(phoneNumber) {
    if (!phoneNumber) return '';
    
    // If it's already in international format, return as is
    if (phoneNumber.startsWith('+')) {
        return phoneNumber;
    }
    
    // For US numbers (assuming 10 digits)
    if (phoneNumber.length === 10) {
        return `(${phoneNumber.substring(0, 3)}) ${phoneNumber.substring(3, 6)}-${phoneNumber.substring(6)}`;
    }
    
    return phoneNumber;
}

// Validate a form element
function validateFormElement(element, pattern, errorMessage) {
    const isValid = !element.value || pattern.test(element.value);
    
    if (!isValid) {
        element.classList.add('is-invalid');
        
        // Add validation message if not already present
        if (!element.nextElementSibling || !element.nextElementSibling.classList.contains('invalid-feedback')) {
            const feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            feedback.textContent = errorMessage;
            element.parentNode.insertBefore(feedback, element.nextSibling);
        }
    } else {
        element.classList.remove('is-invalid');
    }
    
    return isValid;
}

// Validate required field
function validateRequired(element, errorMessage) {
    const isValid = element.value && element.value.trim() !== '';
    
    if (!isValid) {
        element.classList.add('is-invalid');
        
        // Add validation message if not already present
        if (!element.nextElementSibling || !element.nextElementSibling.classList.contains('invalid-feedback')) {
            const feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            feedback.textContent = errorMessage;
            element.parentNode.insertBefore(feedback, element.nextSibling);
        }
    } else {
        element.classList.remove('is-invalid');
    }
    
    return isValid;
}

// Validate entire form
function validateForm(form) {
    const nameInput = form.querySelector('[name="name"]') || form.querySelector('#editName');
    const emailInput = form.querySelector('[name="email"]') || form.querySelector('#editEmail');
    const phoneInput = form.querySelector('[name="phoneNumber"]') || form.querySelector('#editPhoneNumber');
    
    const isNameValid = validateRequired(nameInput, 'Name is required');
    const isEmailValid = validateRequired(emailInput, 'Email is required') && 
                         validateFormElement(emailInput, EMAIL_PATTERN, 'Please enter a valid email address');
    const isPhoneValid = !phoneInput.value || validateFormElement(phoneInput, PHONE_PATTERN, 
                         'Please enter a valid phone number in international format (e.g., +12345678901)');
    
    return isNameValid && isEmailValid && isPhoneValid;
}

// Create a new member
async function createMember(memberData) {
    try {
        const response = await fetch(`${API_URL}/members`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(memberData)
        });
        
        if (response.ok) {
            showSuccess('Member registered successfully!');
            memberForm.reset();
            loadMembers();
        } else {
            const errorData = await response.json();
            showError(errorData.message || 'Failed to register member.');
        }
    } catch (error) {
        console.error('Error creating member:', error);
        showError('Failed to register member. Please try again later.');
    }
}

// Update a member
async function updateMember(id, memberData) {
    try {
        const response = await fetch(`${API_URL}/members/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(memberData)
        });
        
        if (response.ok) {
            showSuccess('Member updated successfully!');
            editMemberModal.hide();
            loadMembers();
        } else {
            const errorData = await response.json();
            showError(errorData.message || 'Failed to update member.');
        }
    } catch (error) {
        console.error('Error updating member:', error);
        showError('Failed to update member. Please try again later.');
    }
}

// Delete a member
async function deleteMember(id) {
    if (confirm('Are you sure you want to delete this member?')) {
        try {
            const response = await fetch(`${API_URL}/members/${id}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                showSuccess('Member deleted successfully!');
                loadMembers();
            } else {
                const errorData = await response.json();
                showError(errorData.message || 'Failed to delete member.');
            }
        } catch (error) {
            console.error('Error deleting member:', error);
            showError('Failed to delete member. Please try again later.');
        }
    }
}

// Search members by name
async function searchMembers(name) {
    try {
        const response = await fetch(`${API_URL}/members/search?name=${encodeURIComponent(name)}`);
        const members = await response.json();
        
        if (members.length === 0) {
            membersTableBody.innerHTML = '';
            noMembersAlert.classList.remove('d-none');
        } else {
            noMembersAlert.classList.add('d-none');
            renderMembersTable(members);
        }
    } catch (error) {
        console.error('Error searching members:', error);
        showError('Failed to search members. Please try again later.');
    }
}

// Load member for editing
async function editMember(id) {
    try {
        const response = await fetch(`${API_URL}/members/${id}`);
        const member = await response.json();
        
        document.getElementById('editId').value = member.id;
        document.getElementById('editName').value = member.name;
        document.getElementById('editEmail').value = member.email;
        document.getElementById('editPhoneNumber').value = member.phoneNumber || '';
        
        // Clear any previous validation states
        document.querySelectorAll('#editMemberForm .is-invalid').forEach(el => {
            el.classList.remove('is-invalid');
        });
        
        editMemberModal.show();
    } catch (error) {
        console.error('Error loading member for edit:', error);
        showError('Failed to load member details. Please try again later.');
    }
}

// Show error message
function showError(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'alert alert-danger mt-3';
    errorDiv.innerHTML = message; // Using innerHTML to support line breaks
    
    // Find the appropriate place to show the error
    const container = document.querySelector('.container');
    container.insertBefore(errorDiv, container.firstChild);
    
    // Remove after 5 seconds
    setTimeout(() => {
        errorDiv.remove();
    }, 5000);
}

// Show success message
function showSuccess(message) {
    const successDiv = document.createElement('div');
    successDiv.className = 'alert alert-success mt-3';
    successDiv.textContent = message;
    
    // Find the appropriate place to show the success message
    const container = document.querySelector('.container');
    container.insertBefore(successDiv, container.firstChild);
    
    // Remove after 5 seconds
    setTimeout(() => {
        successDiv.remove();
    }, 5000);
}

// Setup real-time form validation
function setupFormValidation() {
    // Add blur event listeners to form fields
    const nameInput = document.getElementById('name');
    const emailInput = document.getElementById('email');
    const phoneInput = document.getElementById('phoneNumber');
    
    nameInput.addEventListener('blur', () => {
        validateRequired(nameInput, 'Name is required');
    });
    
    emailInput.addEventListener('blur', () => {
        validateRequired(emailInput, 'Email is required');
        if (emailInput.value) {
            validateFormElement(emailInput, EMAIL_PATTERN, 'Please enter a valid email address');
        }
    });
    
    phoneInput.addEventListener('blur', () => {
        if (phoneInput.value) {
            validateFormElement(phoneInput, PHONE_PATTERN, 
                'Please enter a valid phone number in international format (e.g., +12345678901)');
        }
    });
    
    // Same for edit form
    const editNameInput = document.getElementById('editName');
    const editEmailInput = document.getElementById('editEmail');
    const editPhoneInput = document.getElementById('editPhoneNumber');
    
    editNameInput.addEventListener('blur', () => {
        validateRequired(editNameInput, 'Name is required');
    });
    
    editEmailInput.addEventListener('blur', () => {
        validateRequired(editEmailInput, 'Email is required');
        if (editEmailInput.value) {
            validateFormElement(editEmailInput, EMAIL_PATTERN, 'Please enter a valid email address');
        }
    });
    
    editPhoneInput.addEventListener('blur', () => {
        if (editPhoneInput.value) {
            validateFormElement(editPhoneInput, PHONE_PATTERN, 
                'Please enter a valid phone number in international format (e.g., +12345678901)');
        }
    });
}

// Event Listeners
document.addEventListener('DOMContentLoaded', () => {
    loadMembers();
    setupFormValidation();
    
    memberForm.addEventListener('submit', (e) => {
        e.preventDefault();
        
        // Validate form before submission
        if (validateForm(memberForm)) {
            const memberData = {
                name: document.getElementById('name').value,
                email: document.getElementById('email').value,
                phoneNumber: document.getElementById('phoneNumber').value
            };
            
            createMember(memberData);
        }
    });
    
    searchButton.addEventListener('click', () => {
        const searchTerm = searchInput.value.trim();
        if (searchTerm) {
            searchMembers(searchTerm);
        } else {
            loadMembers();
        }
    });
    
    clearButton.addEventListener('click', () => {
        searchInput.value = '';
        loadMembers();
    });
    
    saveEditButton.addEventListener('click', () => {
        // Validate edit form before submission
        if (validateForm(editMemberForm)) {
            const id = document.getElementById('editId').value;
            const memberData = {
                name: document.getElementById('editName').value,
                email: document.getElementById('editEmail').value,
                phoneNumber: document.getElementById('editPhoneNumber').value
            };
            
            updateMember(id, memberData);
        }
    });
}); 