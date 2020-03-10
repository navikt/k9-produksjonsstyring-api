package no.nav.k9.tilgangskontroll.abac.mapper;

import com.google.gson.reflect.TypeToken;
import no.nav.k9.tilgangskontroll.abac.Advice;
import no.nav.k9.tilgangskontroll.abac.AttributeAssignment;
import no.nav.k9.tilgangskontroll.abac.Response;

import java.lang.reflect.Type;
import java.util.List;

public class JsonMapperTypes {
    public static final Type responseType = new TypeToken<List<Response>>() {
    }.getType();
    public static final Type associatedAdviceType = new TypeToken<List<Advice>>() {
    }.getType();
    public static final Type attributeAssignmentType = new TypeToken<List<AttributeAssignment>>() {
    }.getType();
}
