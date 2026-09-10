package ru.practicum.telemetry.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sensors", schema = "public")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
    @Id
    private String id;

    private String hubId;
}
