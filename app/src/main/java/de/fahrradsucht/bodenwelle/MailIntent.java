package de.fahrradsucht.bodenwelle;

import android.content.Intent;
import android.net.Uri;
import java.net.IDN;

/** ACTION_SENDTO opens an editable draft. It never sends a message itself. */
public final class MailIntent {
    private MailIntent() {}
    public static boolean validAddress(String text) {
        if (text == null || text.length() > 254 || !text.equals(text.trim()) || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) return false;
        String[] parts = text.split("@", -1);
        if (parts.length != 2 || !parts[0].matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+") || parts[0].startsWith(".") || parts[0].endsWith(".") || parts[0].contains("..")) return false;
        try {
            String[] labels = IDN.toASCII(parts[1], IDN.USE_STD3_ASCII_RULES).split("\\.", -1);
            if (labels.length < 2 || labels[labels.length - 1].length() < 2) return false;
            for (String label : labels) if (!label.matches("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?")) return false;
            return true;
        }
        catch (IllegalArgumentException e) { return false; }
    }
    public static Intent create(String recipient, String subject, String body) {
        if (!validAddress(recipient)) throw new IllegalArgumentException("Bitte eine einzelne gültige E-Mail-Adresse eingeben.");
        if (subject.indexOf('\n') >= 0 || subject.indexOf('\r') >= 0) throw new IllegalArgumentException("Der Betreff darf keinen Zeilenumbruch enthalten.");
        // Keep all content in the mailto URI: many mail clients ignore SENDTO extras.
        Uri uri = Uri.parse("mailto:" + Uri.encode(recipient, "@") + "?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body));
        if (uri.toString().length() > 150_000) throw new IllegalArgumentException("Der Entwurf ist zu lang. Bitte weniger Stellen auswählen oder den Text kürzen.");
        return new Intent(Intent.ACTION_SENDTO, uri).putExtra(Intent.EXTRA_EMAIL, new String[]{recipient})
                .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, body);
    }
    public static Intent withPhotos(String recipient,String subject,String body,java.util.ArrayList<Uri> photos) {
        if(photos.isEmpty())return create(recipient,subject,body);
        create(recipient,subject,body); // Reuse recipient/header/size checks.
        Intent mail=new Intent(photos.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE).setType("message/rfc822")
                .putExtra(Intent.EXTRA_EMAIL,new String[]{recipient}).putExtra(Intent.EXTRA_SUBJECT,subject).putExtra(Intent.EXTRA_TEXT,body);
        if(photos.size()==1)mail.putExtra(Intent.EXTRA_STREAM,photos.get(0));else mail.putParcelableArrayListExtra(Intent.EXTRA_STREAM,photos);
        android.content.ClipData clip=new android.content.ClipData("Fotos zu Fundstellen",new String[]{"image/jpeg"},new android.content.ClipData.Item(photos.get(0)));
        for(int i=1;i<photos.size();i++)clip.addItem(new android.content.ClipData.Item(photos.get(i)));
        mail.setClipData(clip);mail.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mail.setSelector(new Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:"+Uri.encode(recipient,"@"))));
        return mail;
    }
}
