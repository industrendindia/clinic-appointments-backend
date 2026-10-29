package in.carenest.clinic.api;

import in.carenest.clinic.security.SecurityConfig.TenantPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class OperationsApi {
  private final JdbcTemplate db;
  public OperationsApi(JdbcTemplate db){this.db=db;}

  record RoomInput(@NotBlank String name,@NotBlank String roomType){}
  @GetMapping("/rooms") List<Map<String,Object>> rooms(@AuthenticationPrincipal TenantPrincipal p){tenant(p);return db.queryForList("select id,name,room_type,active from rooms where clinic_id=? and branch_id=? order by name",p.clinicId(),p.branchId());}
  @PostMapping("/rooms") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> addRoom(@AuthenticationPrincipal TenantPrincipal p,@Valid @RequestBody RoomInput x){owner(p);UUID id=UUID.randomUUID();db.update("insert into rooms(id,clinic_id,branch_id,name,room_type) values(?,?,?,?,?)",id,p.clinicId(),p.branchId(),x.name(),x.roomType());return Map.of("id",id);}

  record ServiceInput(@NotBlank String name,@NotBlank String category,@NotBlank String roomType,@Min(15) int minutes,@DecimalMin("0.00") BigDecimal price,@DecimalMin("0.00") BigDecimal discountPercent){}
  @PostMapping("/services") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> addService(@AuthenticationPrincipal TenantPrincipal p,@Valid @RequestBody ServiceInput x){owner(p);UUID id=UUID.randomUUID();db.update("insert into services(id,clinic_id,name,category,room_type,minutes,price,discount_percent) values(?,?,?,?,?,?,?,?)",id,p.clinicId(),x.name(),x.category(),x.roomType(),x.minutes(),x.price(),x.discountPercent());return Map.of("id",id);}

  record ProductInput(@NotBlank String name,String description,@DecimalMin("0.00") BigDecimal price,@Min(0) int stockQuantity){}
  @GetMapping("/products") List<Map<String,Object>> products(@AuthenticationPrincipal TenantPrincipal p){tenant(p);return db.queryForList("select id,name,description,price,stock_quantity,active from products where clinic_id=? order by name",p.clinicId());}
  @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> addProduct(@AuthenticationPrincipal TenantPrincipal p,@Valid @RequestBody ProductInput x){owner(p);UUID id=UUID.randomUUID();db.update("insert into products(id,clinic_id,name,description,price,stock_quantity) values(?,?,?,?,?,?)",id,p.clinicId(),x.name(),x.description(),x.price(),x.stockQuantity());return Map.of("id",id);}

  record PackageInput(@NotBlank String name,@NotNull UUID serviceId,@Min(1) int sessionCount,@DecimalMin("0.00") BigDecimal price){}
  @GetMapping("/packages") List<Map<String,Object>> packages(@AuthenticationPrincipal TenantPrincipal p){tenant(p);return db.queryForList("select p.id,p.name,p.session_count,p.price,p.active,s.name service from packages p left join services s on s.id=p.service_id where p.clinic_id=? order by p.name",p.clinicId());}
  @PostMapping("/packages") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> addPackage(@AuthenticationPrincipal TenantPrincipal p,@Valid @RequestBody PackageInput x){owner(p);UUID id=UUID.randomUUID();db.update("insert into packages(id,clinic_id,name,service_id,session_count,price) select ?,?,?,?,?,? where exists(select 1 from services where id=? and clinic_id=?)",id,p.clinicId(),x.name(),x.serviceId(),x.sessionCount(),x.price(),x.serviceId(),p.clinicId());return Map.of("id",id);}

  @GetMapping("/practitioners") List<Map<String,Object>> practitioners(@AuthenticationPrincipal TenantPrincipal p){tenant(p);return db.queryForList("select pr.id,pr.full_name,pr.engagement,pr.years_experience,pr.rate,pr.salary,pr.active,u.username,u.phone from practitioners pr left join users u on u.id=pr.user_id where pr.clinic_id=? and pr.branch_id=? order by pr.full_name",p.clinicId(),p.branchId());}

  @GetMapping("/appointments") List<Map<String,Object>> appointments(@AuthenticationPrincipal TenantPrincipal p,@RequestParam(required=false) LocalDate date){tenant(p);LocalDate d=date==null?db.queryForObject("select current_date",LocalDate.class):date;return db.queryForList("select a.id,a.starts_at,a.ends_at,a.status,a.price,a.attendance_comment,c.full_name customer,pr.full_name practitioner,s.name service,r.name room from appointments a join customers c on c.id=a.customer_id join practitioners pr on pr.id=a.practitioner_id join services s on s.id=a.service_id join rooms r on r.id=a.room_id where a.clinic_id=? and a.branch_id=? and a.starts_at>=? and a.starts_at<? order by a.starts_at",p.clinicId(),p.branchId(),d.atStartOfDay(),d.plusDays(1).atStartOfDay());}

  record AttendanceInput(@NotBlank String status,String comment){}
  @PutMapping("/appointments/{id}/attendance") @Transactional Map<String,Object> attendance(@AuthenticationPrincipal TenantPrincipal p,@PathVariable UUID id,@Valid @RequestBody AttendanceInput x){tenant(p);if(!List.of("DONE","NO_SHOW").contains(x.status()))throw error(HttpStatus.BAD_REQUEST,"Status must be DONE or NO_SHOW");int updated=db.update("update appointments set status=?,attendance_comment=?,version=version+1 where id=? and clinic_id=? and branch_id=? and status='BOOKED'",x.status(),x.comment(),id,p.clinicId(),p.branchId());if(updated==0)throw error(HttpStatus.CONFLICT,"Appointment is not available for attendance");return Map.of("id",id,"status",x.status());}

  @GetMapping("/payments") List<Map<String,Object>> payments(@AuthenticationPrincipal TenantPrincipal p,@RequestParam(required=false) UUID customerId){tenant(p);if(customerId==null)return db.queryForList("select p.id,p.amount,p.status,p.method,p.receipt_number,p.confirmed_at,c.full_name customer from payments p join customers c on c.id=p.customer_id where p.clinic_id=? and p.branch_id=? order by p.created_at desc limit 100",p.clinicId(),p.branchId());return db.queryForList("select p.id,p.amount,p.status,p.method,p.receipt_number,p.confirmed_at,c.full_name customer from payments p join customers c on c.id=p.customer_id where p.clinic_id=? and p.branch_id=? and p.customer_id=? order by p.created_at desc",p.clinicId(),p.branchId(),customerId);}

  @GetMapping("/whatsapp/service-menu") Map<String,Object> whatsappMenu(@AuthenticationPrincipal TenantPrincipal p){tenant(p);var clinic=db.queryForMap("select name,whatsapp_phone from clinics where id=?",p.clinicId());var services=db.queryForList("select name,minutes,price from services where clinic_id=? and active order by name",p.clinicId());return Map.of("clinic",clinic,"services",services,"format","NUMBERED_LIST");}

  private void tenant(TenantPrincipal p){if(p==null||p.clinicId()==null)throw error(HttpStatus.FORBIDDEN,"Clinic context required");}
  private void owner(TenantPrincipal p){tenant(p);if(!"OWNER".equals(p.role()))throw error(HttpStatus.FORBIDDEN,"Clinic owner required");}
  private ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
