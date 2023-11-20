package com.gmhis_backk.service;

import com.gmhis_backk.AppUtils;
import com.gmhis_backk.constant.GMHISQuotationStatus;
import com.gmhis_backk.domain.*;
import com.gmhis_backk.domain.quotation.GMHISQuotation;
import com.gmhis_backk.domain.quotation.GMHISQuotationCreate;
import com.gmhis_backk.domain.quotation.GMHISQuotationPartial;
import com.gmhis_backk.domain.quotation.item.GMHISQuotationItemPartial;
import com.gmhis_backk.exception.domain.ResourceNotFoundByIdException;
import com.gmhis_backk.repository.GMHISQuotationRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Transactional
@Service
@Log4j2
public class GMHISQuotationService {

    private final PatientService patientService;

    private final GMHISQuotationRepository quotationRepository;

    private  final GMHISQuotationItemService quotationItemService ;
    private  final InsuranceService insuranceService;

    private final UserService userService;

    public GMHISQuotationService(
            final PatientService patientService,
            final UserService userService,
            final GMHISQuotationRepository quotationRepository,
            final GMHISQuotationItemService quotationItemService,
            final InsuranceService insuranceService
    ) {
        this.patientService = patientService;
        this.userService = userService;
        this.quotationRepository = quotationRepository;
        this.quotationItemService = quotationItemService;
        this.insuranceService = insuranceService;
    }

    protected String generateQuotationNumber() {
        Random rnd = new Random();
        int n = 100000 + rnd.nextInt(900000);
        return "GMHIS-QUO-" + n;
    }
    protected User getCurrentUser() {
        return this.userService.findUserByUsername(AppUtils.getUsername());
    }

    public String updateQuotationStatus(UUID quotationID, String quotationStatus) throws ResourceNotFoundByIdException {
        GMHISQuotation quotationToUpdate = quotationRepository.findById(quotationID)
                .orElseThrow(() -> new ResourceNotFoundByIdException(" le Devis est Inexistant"));
        quotationToUpdate.setStatus(quotationStatus);
        quotationToUpdate.setQuotationNumber(generateQuotationNumber());
        if (quotationToUpdate.getStatus() == null || Objects.equals(quotationStatus, GMHISQuotationStatus.TO_BE_INVOICED)) quotationToUpdate.setQuotationNumber(generateQuotationNumber());

        return quotationRepository.save(quotationToUpdate).getStatus();

    }

    public GMHISQuotationPartial toPartial(GMHISQuotation quotation) {
        GMHISQuotationPartial quotationPartial = new GMHISQuotationPartial();
        quotationPartial.setId(quotation.getId());
        quotationPartial.setCode(quotation.getCode());
        quotationPartial.setQuotationNumber(quotation.getQuotationNumber());
        quotationPartial.setPatientName(new GMHISName(quotation.getPatient().getFirstName(), quotation.getPatient().getLastName()));
        quotationPartial.setPatientID(quotation.getPatient().getId());
        quotationPartial.setStatus(quotation.getStatus());
        quotationPartial.setTotalAmount(quotation.getTotalAmount());
        quotationPartial.setInsuranceID(quotation.getInsuranceId());
        quotationPartial.setInsuranceName(quotation.getInsuranceName());
        quotationPartial.setModeratorTicket(quotation.getModeratorTicket());
        quotationPartial.setDateOp(quotation.getCreatedAt());
        quotationPartial.setAffection(quotation.getAffection());
        quotationPartial.setIndication(quotation.getIndication());
        quotationPartial.setCmuPart(quotation.getCmuPart());
        quotationPartial.setDiscount(quotation.getDiscount());
        quotationPartial.setNetToPay(quotation.getNetToPay());
        quotationPartial.setInsurancePart(quotation.getInsurancePart());
        return   quotationPartial;
    }


    protected List<GMHISQuotationPartial> map(List<GMHISQuotation> quotations) {
        List<GMHISQuotationPartial> quotationPartialList = new ArrayList<>();

        quotations.forEach(quotation -> {
            quotationPartialList.add(toPartial(quotation));
        });

        return quotationPartialList;
    }

    public GMHISQuotationPartial create(GMHISQuotationCreate quotationCreate) throws ResourceNotFoundByIdException {
        GMHISQuotation quotation = new GMHISQuotation();

        Patient patient = patientService.findById(quotationCreate.getPatientID());
        if (patient == null) throw new ResourceNotFoundByIdException("Patient Inexistant");
        quotation.setPatient(patient);

        if (quotationCreate.getInsuranceID() != null){
            Insurance insurance = insuranceService.findInsuranceById(quotationCreate.getInsuranceID()).orElse(null);
            if (insurance != null) {
                quotation.setInsuranceId(insurance.getId());
                quotation.setInsuranceName(insurance.getName());
            } ;
        }

        Random rnd = new Random();
        int n = 100000 + rnd.nextInt(900000);
        quotation.setCode(generateQuotationNumber());
        quotation.setQuotationNumber(generateQuotationNumber());
        quotation.setStatus(GMHISQuotationStatus.DRAFT);
        BeanUtils.copyProperties(quotationCreate,quotation,"id");

        quotation.setCreatedAt(new Date());
        quotation.setCreatedBy(getCurrentUser().getId());
        GMHISQuotation quotationSaved  = quotationRepository.save(quotation);

        quotationCreate.getQuotationItems().forEach(item -> {
            quotationItemService.create(item, quotationSaved.getId());
        });

        return  toPartial(quotationSaved);
    }

    public GMHISQuotationPartial retrieve(UUID quotationID) throws ResourceNotFoundByIdException{
        GMHISQuotation quotation = quotationRepository.findById(quotationID)
                .orElseThrow(() -> new ResourceNotFoundByIdException(" le devis est inexistant"));
        return toPartial(quotation);
    }

    public GMHISQuotationPartial update (UUID quotationID, GMHISQuotationCreate quotationCreate) throws ResourceNotFoundByIdException {
        GMHISQuotation quotationUpdate = quotationRepository.findById(quotationID)
                .orElseThrow(() -> new ResourceNotFoundByIdException(" Le devis est inexistante"));

        if (quotationCreate.getInsuranceID() != null){
            Insurance insurance = insuranceService.findInsuranceById(quotationCreate.getInsuranceID()).orElse(null);
            if (insurance != null) {
                quotationUpdate.setInsuranceId(insurance.getId());
                quotationUpdate.setInsuranceName(insurance.getName());
            } ;
        }

        BeanUtils.copyProperties(quotationCreate,quotationUpdate,"id");
        quotationUpdate.setUpdatededAt(new Date());
        quotationUpdate.setUpdatedBy(getCurrentUser().getId());
        GMHISQuotation quotationSaved  = quotationRepository.save(quotationUpdate);
        List<GMHISQuotationItemPartial> quotationItemsExisting = quotationItemService.findByQuotationID(quotationID);
        quotationItemService.deleteAll(quotationItemsExisting);
        quotationCreate.getQuotationItems().forEach(item -> {
            quotationItemService.create(item, quotationSaved.getId());
        });
        return toPartial(quotationUpdate);
    }

    public ResponseEntity<Map<String, Object>> search(Map<String, ?> devisSearch) {

        Map<String, Object> searchResult = new HashMap<>();

        int page = (int) devisSearch.get("page");
        String[] sort = (String[]) devisSearch.get("sort");
        int size = (int) devisSearch.get("size");

        Sort.Direction dir = sort[1].equalsIgnoreCase("asc") ? dir = Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort[0]));
        Page<GMHISQuotation> quotationPage = null;

        quotationPage = quotationRepository.findAll(pageable);

        List<GMHISQuotation> quotationList = quotationPage.getContent();

        List<GMHISQuotationPartial> quotations = this.map(quotationList);

        searchResult.put("items", quotations);
        searchResult.put("totalElements", quotationPage.getTotalElements());
        searchResult.put("totalPages", quotationPage.getTotalPages());
        searchResult.put("size", quotationPage.getSize());
        searchResult.put("pageNumber", quotationPage.getNumber());
        searchResult.put("numberOfElements", quotationPage.getNumberOfElements());
        searchResult.put("first", quotationPage.isFirst());
        searchResult.put("last", quotationPage.isLast());
        searchResult.put("empty", quotationPage.isEmpty());

        return new ResponseEntity<>(searchResult, HttpStatus.OK);

    }

    }