package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.dto.position.PositionDto;
import com.olisystem.optionsmanager.dto.position.PositionFilterCriteria;
import com.olisystem.optionsmanager.dto.position.PositionSummaryResponseDto;
import com.olisystem.optionsmanager.mapper.PositionMapper;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.util.SecurityUtil;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço especializado em consultas de posições. Implementa diferentes métodos de busca e filtros
 * para posições.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PositionQueryService {

  private final PositionRepository positionRepository;
  private final PositionMapper mapper;
  private final PositionCalculator calculator;

  /** Busca posições pelo status. */
  public PositionSummaryResponseDto findByStatuses(
      List<PositionStatus> statuses, Pageable pageable) {
    log.debug("Buscando posições com status: {}", statuses);

    Page<Position> page =
        positionRepository.findByUserAndStatusIn(SecurityUtil.getLoggedUser(), statuses, pageable);

    List<PositionDto> positionDtos = mapper.toDtoList(page.getContent());

    return calculator.calculateSummary(
        positionDtos,
        page.getNumber(),
        page.getTotalPages(),
        page.getTotalElements(),
        page.getSize());
  }

  /** Busca posições com filtros avançados. */
  public PositionSummaryResponseDto findByFilters(
      PositionFilterCriteria criteria, Pageable pageable) {
    log.debug("Buscando posições com filtros: {}", criteria);

    Specification<Position> spec = createSpecification(criteria);
    Page<Position> page = positionRepository.findAll(spec, pageable);

    List<PositionDto> positionDtos = mapper.toDtoList(page.getContent());

    return calculator.calculateSummary(
        positionDtos,
        page.getNumber(),
        page.getTotalPages(),
        page.getTotalElements(),
        page.getSize());
  }

  /** Cria uma especificação para filtrar posições com base nos critérios. */
  private Specification<Position> createSpecification(PositionFilterCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Filtro por usuário
      predicates.add(cb.equal(root.get("user"), SecurityUtil.getLoggedUser()));

      // Filtro por status
      if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
        predicates.add(root.get("status").in(criteria.getStatus()));
      }

      // Filtro por datas de abertura
      if (criteria.getOpenDateStart() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("openDate"), criteria.getOpenDateStart()));
      }
      if (criteria.getOpenDateEnd() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("openDate"), criteria.getOpenDateEnd()));
      }

      // Filtro por datas de fechamento
      if (criteria.getCloseDateStart() != null) {
        predicates.add(
            cb.greaterThanOrEqualTo(root.get("closeDate"), criteria.getCloseDateStart()));
      }
      if (criteria.getCloseDateEnd() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("closeDate"), criteria.getCloseDateEnd()));
      }

      // Filtro por corretora
      if (criteria.getBrokerageName() != null && !criteria.getBrokerageName().isEmpty()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("brokerage").get("name")),
                "%" + criteria.getBrokerageName().toLowerCase() + "%"));
      }

      // Filtro por direção
      if (criteria.getDirection() != null) {
        predicates.add(cb.equal(root.get("direction"), criteria.getDirection()));
      }

      // Filtro por tipo de opção
      if (criteria.getOptionType() != null) {
        predicates.add(cb.equal(root.get("optionSeries").get("type"), criteria.getOptionType()));
      }

      // Filtro por código da série
      if (criteria.getOptionSeriesCode() != null && !criteria.getOptionSeriesCode().isEmpty()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("optionSeries").get("code")),
                "%" + criteria.getOptionSeriesCode().toLowerCase() + "%"));
      }

      // Filtro por código do ativo base
      if (criteria.getBaseAssetCode() != null && !criteria.getBaseAssetCode().isEmpty()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("optionSeries").get("asset").get("code")),
                "%" + criteria.getBaseAssetCode().toLowerCase() + "%"));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
