package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.DepartmentResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Department;
import com.ziyara.backend.domain.entity.Employee;
import com.ziyara.backend.domain.repository.DepartmentRepository;
import com.ziyara.backend.domain.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock DepartmentRepository departmentRepository;
    @Mock EmployeeRepository employeeRepository;

    DepartmentService service;

    @BeforeEach
    void setUp() {
        service = new DepartmentService(departmentRepository, employeeRepository);
    }

    // ── createDepartment ──────────────────────────────────────────────────────

    @Test
    void createDepartment_newName_savesDepartment() {
        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.empty());
        Department saved = dept("Engineering");
        when(departmentRepository.save(any())).thenReturn(saved);

        DepartmentResponse result = service.createDepartment("Engineering", "Dev team", null);

        assertThat(result.getName()).isEqualTo("Engineering");
        verify(departmentRepository).save(any());
    }

    @Test
    void createDepartment_duplicateName_throwsRuntimeException() {
        when(departmentRepository.findByName("HR")).thenReturn(Optional.of(dept("HR")));

        assertThatThrownBy(() -> service.createDepartment("HR", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    // ── getAllDepartments ─────────────────────────────────────────────────────

    @Test
    void getAllDepartments_returnsMappedList() {
        when(departmentRepository.findAll()).thenReturn(List.of(dept("Finance"), dept("Legal")));

        List<DepartmentResponse> result = service.getAllDepartments();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllDepartments_emptyRepo_returnsEmpty() {
        when(departmentRepository.findAll()).thenReturn(List.of());

        assertThat(service.getAllDepartments()).isEmpty();
    }

    // ── getDepartment ─────────────────────────────────────────────────────────

    @Test
    void getDepartment_found_returnsMapped() {
        UUID id = UUID.randomUUID();
        Department d = dept("Operations");
        d.setId(id);
        when(departmentRepository.findById(id)).thenReturn(Optional.of(d));

        DepartmentResponse result = service.getDepartment(id);

        assertThat(result.getName()).isEqualTo("Operations");
    }

    @Test
    void getDepartment_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDepartment(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateDepartment ──────────────────────────────────────────────────────

    @Test
    void updateDepartment_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDepartment(id, "New Name", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDepartment_partialFields_onlyUpdatesNonNull() {
        UUID id = UUID.randomUUID();
        Department existing = dept("Old Name");
        existing.setId(id);
        existing.setDescription("Old description");
        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any())).thenReturn(existing);

        service.updateDepartment(id, "New Name", null, null);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getDescription()).isEqualTo("Old description");
    }

    @Test
    void updateDepartment_blankName_doesNotChangeName() {
        UUID id = UUID.randomUUID();
        Department existing = dept("Preserved Name");
        existing.setId(id);
        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any())).thenReturn(existing);

        service.updateDepartment(id, "   ", null, null);

        assertThat(existing.getName()).isEqualTo("Preserved Name");
    }

    // ── deleteDepartment ──────────────────────────────────────────────────────

    @Test
    void deleteDepartment_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDepartment(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDepartment_hasEmployees_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(dept("IT")));
        when(employeeRepository.findByDepartmentId(id)).thenReturn(List.of(new Employee()));

        assertThatThrownBy(() -> service.deleteDepartment(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("employees");
    }

    @Test
    void deleteDepartment_noEmployees_deletesSuccessfully() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(dept("Empty Dept")));
        when(employeeRepository.findByDepartmentId(id)).thenReturn(List.of());

        service.deleteDepartment(id);

        verify(departmentRepository).deleteById(id);
    }

    private Department dept(String name) {
        Department d = new Department();
        d.setId(UUID.randomUUID());
        d.setName(name);
        return d;
    }
}
