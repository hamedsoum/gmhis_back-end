package com.gmhis_backk.domain.hospitalization.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GMHISHospitalizationRequestCreate {
    private Long examinationID;

    private Long admissionID;

    private Long insuredID;

    private Long patientID;

    private String reason;

    private String protocole;

    private int dayNumber;

    private Date startDate;
}