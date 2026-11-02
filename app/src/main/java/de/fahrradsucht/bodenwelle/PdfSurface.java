package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Searchable Unicode text, vector tables and explicit links; fonts are bundled for consistent device output. */
final class PdfSurface implements AutoCloseable {
    private final PDDocument document=new PDDocument();private final PDType0Font regular,bold;
    private PDPage page;private PDPageContentStream stream;boolean escapedGlyph;
    PdfSurface(Context context,ReportData data)throws IOException {
        PDFBoxResourceLoader.init(context);
        try(java.io.InputStream font=context.getAssets().open("fonts/NotoSans-Regular.ttf")){regular=PDType0Font.load(document,font,true);}
        try(java.io.InputStream font=context.getAssets().open("fonts/NotoSans-Bold.ttf")){bold=PDType0Font.load(document,font,true);}
        document.getDocumentInformation().setTitle("Fundstellenbericht "+data.reference());
        document.getDocumentInformation().setCreator(data.appNotice()?"Bodenwelle":"Fundstellenbericht");
    }
    void newPage(){try{page=new PDPage(new PDRectangle(595,842));document.addPage(page);stream=new PDPageContentStream(document,page);}catch(IOException e){throw new UncheckedIOException(e);}}
    void finishPage(){try{if(stream!=null){stream.close();stream=null;}}catch(IOException e){throw new UncheckedIOException(e);}}
    String safe(String value,boolean strong) {
        PDType0Font font=strong?bold:regular;StringBuilder result=new StringBuilder();
        value.codePoints().forEach(cp->{String character=new String(Character.toChars(cp));try{font.encode(character);result.append(character);}catch(IOException|IllegalArgumentException missing){result.append(String.format(java.util.Locale.ROOT,"[U+%04X]",cp));escapedGlyph=true;}});
        return result.toString();
    }
    int breakText(String value,float size,float width,boolean strong) {
        int low=0,high=value.length();PDType0Font font=strong?bold:regular;
        try{while(low<high){int mid=(low+high+1)/2;int end=mid;if(end<value.length()&&end>0&&Character.isHighSurrogate(value.charAt(end-1)))end--;if(font.getStringWidth(value.substring(0,end))*size/1000<=width)low=mid;else high=mid-1;}return low;}catch(IOException e){throw new UncheckedIOException(e);}
    }
    void text(String value,float size,float x,float baseline,int color,boolean strong) {
        try{stream.beginText();stream.setFont(strong?bold:regular,size);stream.setNonStrokingColor(Color.red(color),Color.green(color),Color.blue(color));stream.newLineAtOffset(x,842-baseline);stream.showText(safe(value,strong));stream.endText();}catch(IOException e){throw new UncheckedIOException(e);}
    }
    void rect(float l,float t,float r,float b,int color){try{stream.setNonStrokingColor(Color.red(color),Color.green(color),Color.blue(color));stream.addRect(l,842-b,r-l,b-t);stream.fill();}catch(IOException e){throw new UncheckedIOException(e);}}
    void image(Bitmap bitmap,RectF box){try{stream.drawImage(JPEGFactory.createFromImage(document,bitmap,.9f),box.left,842-box.bottom,box.width(),box.height());}catch(IOException e){throw new UncheckedIOException(e);}}
    void link(RectF box,String url){try{PDAnnotationLink link=new PDAnnotationLink();link.setRectangle(new PDRectangle(box.left,842-box.bottom,box.width(),box.height()));PDActionURI action=new PDActionURI();action.setURI(url);link.setAction(action);PDBorderStyleDictionary border=new PDBorderStyleDictionary();border.setWidth(0);link.setBorderStyle(border);page.getAnnotations().add(link);}catch(IOException e){throw new UncheckedIOException(e);}}
    void save(File output)throws IOException{document.save(output);}
    @Override public void close(){try{finishPage();document.close();}catch(IOException e){throw new UncheckedIOException(e);}}
}
