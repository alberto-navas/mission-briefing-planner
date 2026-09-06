package com.albertonavas.missionbriefing.server.search;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Briefing generado de una mision, indexado en ElasticSearch para poder buscar en el
 * archivo historico por texto libre (objetivo, notas, recursos) en vez de solo por id.
 */
// createIndex=false: evita que el contexto de Spring necesite un ElasticSearch vivo solo
// para arrancar (tests, entornos sin el stack completo). El indice se crea solo en el
// primer save() gracias a la creacion automatica por defecto de ElasticSearch.
@Document(indexName = "briefings", createIndex = false)
public class BriefingDocument {

    @Id
    private String id;

    private Long missionId;
    private String missionName;

    @Field(type = FieldType.Text)
    private String summary;

    // Sin este mapeo explicito, ElasticSearch infiere un tipo para "generatedAt" a partir
    // del primer documento indexado y Spring Data no siempre sabe deserializarlo de vuelta
    // a Instant (falla con ConverterNotFoundException al leer). epoch_millis es inequivoco.
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant generatedAt;

    protected BriefingDocument() {
        // requerido por Spring Data
    }

    public BriefingDocument(Long missionId, String missionName, String summary, Instant generatedAt) {
        this.missionId = missionId;
        this.missionName = missionName;
        this.summary = summary;
        this.generatedAt = generatedAt;
    }

    public String getId() {
        return id;
    }

    public Long getMissionId() {
        return missionId;
    }

    public String getMissionName() {
        return missionName;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
