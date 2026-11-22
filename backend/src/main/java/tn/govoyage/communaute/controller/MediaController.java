package tn.govoyage.communaute.controller;

import java.io.IOException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.govoyage.security.CompteAuthentifie;
import tn.govoyage.sejour.dao.SejourDAO;
import static tn.govoyage.sejour.service.SejourService.*;

/** Importe des medias limites et sert uniquement ceux publies ou appartenant au compte. */
@RestController
@RequestMapping("/api/medias")
public class MediaController {
    private final SejourDAO dao;
    /** Recoit le stockage des medias. */
    public MediaController(SejourDAO dao) { this.dao=dao; }
    /** Accepte JPEG, PNG et MP4 apres verification de leur signature. */
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> importer(@AuthenticationPrincipal CompteAuthentifie c,@RequestParam MultipartFile fichier) throws IOException {
        if (fichier.isEmpty() || fichier.getSize()>10_000_000) erreur("Le média doit peser moins de 10 Mo.");
        byte[] octets=fichier.getBytes();
        String type=type(octets);
        String id=UUID.randomUUID().toString();
        dao.ecrire("INSERT INTO media_voyage(id_m,utilisateur_id,type_contenu,contenu) VALUES(?,?,?,?)",id,c.id(),type,octets);
        return Map.of("url","/api/medias/"+id,"type",type.startsWith("image/")?"PHOTO":"VIDEO");
    }
    /** Verifie les premiers octets avant de publier un type MIME. */
    private String type(byte[] b) {
        if (b.length>8 && b[0]==(byte)0x89 && b[1]==0x50 && b[2]==0x4e && b[3]==0x47) return "image/png";
        if (b.length>3 && b[0]==(byte)0xff && b[1]==(byte)0xd8 && b[2]==(byte)0xff) return "image/jpeg";
        if (b.length>12 && b[4]=='f' && b[5]=='t' && b[6]=='y' && b[7]=='p') return "video/mp4";
        erreur("Choisissez une image JPEG/PNG ou une vidéo MP4.");
        return "";
    }
    /** Sert le fichier sans permettre l'execution de contenu actif. */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> lire(@PathVariable String id,@AuthenticationPrincipal CompteAuthentifie c) {
        Map<String,Object> media=unique(dao.lire("""
            SELECT * FROM media_voyage m WHERE id_m=? AND
            (utilisateur_id=? OR EXISTS(SELECT 1 FROM publication p WHERE p.media_url=? AND p.statut='PUBLIEE'))
            """,id,c==null?-1L:c.id(),"/api/medias/"+id));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(media.get("type_contenu").toString()))
            .header("X-Content-Type-Options","nosniff").body((byte[])media.get("contenu"));
    }
}
