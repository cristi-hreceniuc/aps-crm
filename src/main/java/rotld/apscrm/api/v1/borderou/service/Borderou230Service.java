package rotld.apscrm.api.v1.borderou.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.borderou.repository.Borderou;
import rotld.apscrm.api.v1.borderou.repository.BorderouRepository;
import rotld.apscrm.api.v1.borderou.repository.CrmSettingRepository;
import rotld.apscrm.api.v1.f230.repository.F230;
import rotld.apscrm.api.v1.f230.repository.F230Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Borderou230Service {

    private final BorderouRepository borderouRepo;
    private final CrmSettingRepository settingsRepo;
    private final F230Repository f230Repo;

    private String getSetting(String key, String def){
        return settingsRepo.findByName(key).map(s -> (s.getValue()!=null && !s.getValue().isBlank()) ? s.getValue() : s.getDefaultValue())
                .orElse(def);
    }

    private static String xmlEscape(String s){
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&apos;");
    }

    @Transactional
    public GeneratedXml generateForIds(List<Integer> ids, LocalDate dataBorderou){
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("Lista de ID-uri este goală.");

        // Config din crm_settings
        String xmlns          = getSetting("xmlns",          "mfp:anaf:dgti:b230:declaratie:v1");
        String schemaLocation = getSetting("schemaLocation", "mfp:anaf:dgti:b230:declaratie:v1 B230.xsd");
        String xmlLunaStr     = getSetting("xml_luna",       String.valueOf(dataBorderou.getMonthValue()));
        String xmlAnStr       = getSetting("xml_an",         String.valueOf(dataBorderou.getYear()));
        String den            = getSetting("xml_nume",       "Asociația ACȚIUNE PENTRU SĂNĂTATE");
        String cifEntitate    = getSetting("xml_cif",        getSetting("xml_cui", "43771157"));

        int xmlLuna = Integer.parseInt(xmlLunaStr.trim());
        int xmlAn   = Integer.parseInt(xmlAnStr.trim());

        // Date selectate (în ordinea ID-urilor date)
        List<F230> rows = f230Repo.findAllById(ids);
        // re-ordonează ca în input
        Map<Integer, F230> map = rows.stream().collect(Collectors.toMap(F230::getId, r -> r));
        List<F230> ordered = ids.stream().map(map::get).filter(Objects::nonNull).toList();

        // Formate
        String dataAttr = dataBorderou.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        // Construim XML în memorie (id-ul borderoului îl aflăm după insert -> auto-increment)
        StringBuilder sb = new StringBuilder(16384);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        // Inserăm temporar nr_borderou="0", îl înlocuim după ce avem id-ul real (sau refacem stringul)
        sb.append("<borderou230 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
                .append(" xmlns=\"").append(xmlEscape(xmlns)).append("\"")
                .append(" xsi:schemaLocation=\"").append(xmlEscape(schemaLocation)).append("\"")
                .append(" nr_borderou=\"").append("0").append("\"")
                .append(" data_borderou=\"").append(xmlEscape(dataAttr)).append("\"")
                .append(" luna=\"").append(xmlLuna).append("\"")
                .append(" an=\"").append(xmlAn).append("\"")
                .append(" den=\"").append(xmlEscape(den)).append("\"")
                .append(" den_i=\"").append(xmlEscape(den)).append("\"")
                .append(" adresa_i=\"").append(xmlEscape(den)).append("\"")   // dacă vrei alt câmp pentru adresă, schimbă aici
                .append(" cui=\"").append(xmlEscape(cifEntitate)).append("\"")
                .append(" cif_i=\"").append(xmlEscape(cifEntitate)).append("\"")
                .append(" totalPlata_A=\"").append(ordered.size()).append("\">").append("\n\n");

        int nrPoz = 1;
        for (F230 r : ordered){
            String cif_c   = r.getCnp(); // meta 'cnp'; dacă e null, pune gol
            String nume    = r.getFirstName();
            String prenume = r.getLastName();
            String iban    = r.getIban();
            boolean acord  = "1".equals((r.getAcordEmail()==null?"":r.getAcordEmail()).trim());
            boolean twoY   = "1".equals((r.getDistrib2()==null?"":r.getDistrib2()).trim());

            sb.append("  <declaratie230")
                    .append(" cif_c=\"").append(xmlEscape(cif_c)).append("\"")
                    .append(" nr_poz=\"").append(nrPoz).append("\"")
                    .append(" nume_c=\"").append(xmlEscape(nume)).append("\"")
                    .append(" prenume_c=\"").append(xmlEscape(prenume)).append("\">").append("\n")
                    .append("    <bursa_entit bifa_entitate=\"1\"")
                    .append(" den_entitate=\"").append(xmlEscape(den)).append("\"")
                    .append(" cif_entitate=\"").append(xmlEscape(cifEntitate)).append("\"")
                    .append(" cont_entitate=\"").append(xmlEscape(iban)).append("\"")
                    .append(" acord=\"").append(acord ? "1" : "0").append("\"")
                    .append(" valabilitate_distribuire=\"").append(twoY ? "2" : "1").append("\"")
                    .append("/>").append("\n")
                    .append("  </declaratie230>").append("\n");
            nrPoz++;
        }

        sb.append("</borderou230>");

        // Salvăm în borderouri pentru a obține id-ul (nr_borderou)
        Borderou entity = Borderou.builder()
                .dataBorderou(dataBorderou)
                .xml(sb.toString()) // temporar fără nr real
                .build();
        entity = borderouRepo.save(entity);
        Integer borderouId = entity.getId();

        // Actualizăm nr_borderou pe postările selectate (suprascrie existing)
        borderouRepo.setBorderouForIds(ids, borderouId);

        // Re-scriem XML cu nr_borderou corect (sau înlocuire simplă)
        String xml = entity.getXml().replace("nr_borderou=\"0\"", "nr_borderou=\"" + borderouId + "\"");
        entity.setXml(xml);
        borderouRepo.save(entity);

        return new GeneratedXml(borderouId, xml, dataBorderou);
    }

    public record GeneratedXml(Integer id, String xml, LocalDate date) {}
}