package com.achieveplusbe.service;

import com.achieveplusbe.dto.TaskDTO;
import com.achieveplusbe.exception.ResourceNotFoundException;
import com.achieveplusbe.exception.UnauthorizedException;
import com.achieveplusbe.model.Task;
import com.achieveplusbe.model.User;
import com.achieveplusbe.repository.TaskRepository;
import com.achieveplusbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return convertToDTO(task);
    }

    public List<TaskDTO> getTasksByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return taskRepository.findByAssignedUser(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getCurrentUserTasks() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return taskRepository.findByAssignedUser(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        Task task = convertToEntity(taskDTO);
        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        // Update the task
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setStatus(Task.TaskStatus.valueOf(taskDTO.getStatus()));
        task.setDueDate(taskDTO.getDueDate());
        task.setPoints(taskDTO.getPoints());

        if (taskDTO.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(taskDTO.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + taskDTO.getAssignedTo()));
            task.setAssignedUser(assignedUser);
        } else {
            task.setAssignedUser(null);
        }

        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Transactional
    public TaskDTO updateTaskStatus(Long id, String status) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        // Check if the task is assigned to the current user
        if (task.getAssignedUser() == null || !task.getAssignedUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this task");
        }

        task.setStatus(Task.TaskStatus.valueOf(status));
        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        taskRepository.delete(task);
    }

    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus().name());
        dto.setDueDate(task.getDueDate());
        dto.setPoints(task.getPoints());
        dto.setCreatedAt(task.getCreatedAt().format(DATE_FORMATTER));

        if (task.getAssignedUser() != null) {
            dto.setAssignedTo(task.getAssignedUser().getId());
            dto.setAssignedToName(task.getAssignedUser().getFullName());
        }

        return dto;
    }

    private Task convertToEntity(TaskDTO dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(Task.TaskStatus.valueOf(dto.getStatus()));
        task.setDueDate(dto.getDueDate());
        task.setPoints(dto.getPoints());

        if (dto.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(dto.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getAssignedTo()));
            task.setAssignedUser(assignedUser);
        }

        return task;
    }
}