package it.vavassori.trafila;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import android.text.InputType;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, content;
    private EditText startInput, endInput, speedInput;
    private Spinner stepsInput;
    private double start, end, speed;
    private int steps;
    private double[] reductions;
    private boolean[] fixed;
    private boolean updating = false;
    private TextView status, production;
    private final int blue = Color.rgb(20, 76, 126);
    private final int green = Color.rgb(22, 115, 79);
    private final Locale it = Locale.ITALY;

    @Override public void onCreate(Bundle b) { super.onCreate(b); showInput(); }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private String fmt(double x, int digits) { return String.format(it, "% .".trim() + digits + "f", x); }
    private TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(28,38,48));
        if (bold) t.setTypeface(null, 1); t.setPadding(0,dp(6),0,dp(6)); return t;
    }
    private void init(String title) {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(28));
        root.setBackgroundColor(Color.rgb(246,248,251)); scroll.addView(root);
        TextView heading = text(title, 24, true); heading.setTextColor(blue); root.addView(heading);
        content = root; setContentView(scroll);
    }
    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(v -> action.run());
        content.addView(b, new LinearLayout.LayoutParams(-1,dp(54))); return b;
    }
    private EditText field(String label, String initial) {
        content.addView(text(label,16,true)); EditText e = new EditText(this); e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setText(initial); e.setSelectAllOnFocus(true); content.addView(e, new LinearLayout.LayoutParams(-1,dp(52))); return e;
    }
    private double parse(EditText e) { return Double.parseDouble(e.getText().toString().trim().replace(',','.')); }
    private void alert(String title, String msg) { new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("OK",null).show(); }
    private void showInput() {
        init("Calcolo Passaggi Trafila");
        content.addView(text("Acciaio basso carbonio • trafilatura a secco • multipasso a tiro rettilineo",14,false));
        startInput = field("Diametro vergella in entrata (mm)", start>0?fmt(start,2):"5,50");
        endInput = field("Diametro finale (mm)",end>0?fmt(end,2):"2,50");
        content.addView(text("Numero passaggi (1–7)",16,true));
        stepsInput = new Spinner(this); String[] nums = {"1","2","3","4","5","6","7"};
        stepsInput.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,nums));
        stepsInput.setSelection(steps>0?steps-1:4); content.addView(stepsInput);
        speedInput = field("Velocità ultimo blocco (m/s)",speed>0?fmt(speed,2):"10,00");
        content.addView(text("Rendimento produttivo fisso: 80%",14,false));
        button("CALCOLA", () -> {
            try {
                start=parse(startInput); end=parse(endInput); speed=parse(speedInput);
                steps=stepsInput.getSelectedItemPosition()+1;
                if (!(start>0 && end>0 && end<start && speed>0) || !Double.isFinite(start+end+speed)) {
                    alert("Dati non validi","Inserisci diametri positivi, diametro finale inferiore a quello iniziale e velocità positiva."); return;
                }
                double required = 1-Math.pow(end/start,2.0/steps);
                if (required < .18-1e-10) {
                    alert("Sequenza impossibile", "Con questi diametri e "+steps+" passaggi servirebbe almeno un passaggio inferiore al 18%. Aumenta la riduzione totale o diminuisci il numero di passaggi."); return;
                }
                reductions = new double[steps]; fixed = new boolean[steps];
                if (!solve(-1,0)) { alert("Errore","Impossibile distribuire le riduzioni."); return; }
                showResults();
            } catch(Exception ex) { alert("Dati non validi","Controlla i numeri inseriti."); }
        });
        button("CARICA CALCOLO SALVATO", this::load);
    }
    // Vincolo esatto: prodotto(1-r_i) = (D_finale/D_iniziale)^2.
    // Riduzioni decrescenti proposte tramite pesi, con minimo inderogabile 18%.
    // Le percentuali fissate dall'utente rimangono invariate; le altre sono ricalcolate.
    private boolean solve(int changed, double value) {
        if (changed>=0) { reductions[changed]=value; fixed[changed]=true; }
        double target = 2*Math.log(end/start);
        double sumFixed = 0;
        int free = 0;
        for(int i=0;i<steps;i++) {
            if (fixed[i]) sumFixed += Math.log1p(-reductions[i]); else free++;
        }
        if (free==0) return Math.abs(sumFixed-target)<1e-7;
        double needed = target-sumFixed;
        double minimumLog = free*Math.log(.82);
        if (needed > minimumLog+1e-9) return false;
        // weighted log reductions: early stages stronger than late stages
        double[] w = new double[steps];
        for(int i=0;i<steps;i++) w[i] = 1.20 - 0.40*i/Math.max(1,steps-1);
        double low=0, high=100;
        for(int k=0;k<90;k++) {
            double mid=(low+high)/2, s=0;
            for(int i=0;i<steps;i++) if(!fixed[i]) s += Math.min(Math.log(.82), -mid*w[i]);
            if(s>needed) low=mid; else high=mid;
        }
        for(int i=0;i<steps;i++) if(!fixed[i]) reductions[i]=1-Math.exp(Math.min(Math.log(.82),-high*w[i]));
        return true;
    }
    private double[] diameters() {
        double[] d = new double[steps+1]; d[0]=start;
        for(int i=0;i<steps;i++) d[i+1]=d[i]*Math.sqrt(1-reductions[i]);
        return d;
    }
    private void showResults() {
        init("Risultati trafilatura");
        content.addView(text("Vergella "+fmt(start,2)+" mm → filo "+fmt(end,2)+" mm | "+steps+" passaggi",16,true));
        status = text("",14,true); content.addView(status);
        LinearLayout stageContainer = new LinearLayout(this); stageContainer.setOrientation(LinearLayout.VERTICAL); content.addView(stageContainer);
        production = text("",18,true);
        refreshStages(stageContainer);
        content.addView(production);
        button("RIPRISTINA CALCOLO", () -> { Arrays.fill(fixed,false); solve(-1,0); refreshStages(stageContainer); });
        button("SALVA CALCOLO", this::save);
        button("MODIFICA DATI INIZIALI", this::showInput);
        content.addView(text("Nota: riduzioni proposte a scopo di calcolo; verificare filiere, lubrificazione, macchina e condizioni del filo. Oltre il 34% viene mostrato un avviso.",13,false));
    }
    private void refreshStages(LinearLayout stageContainer) {
        if (updating) return;
        updating=true; stageContainer.removeAllViews();
        double[] d=diameters();
        boolean high=false;
        for(int i=0;i<steps;i++) {
            final int index=i;
            LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(dp(12),dp(10),dp(12),dp(10)); panel.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1,-2); pp.setMargins(0,dp(5),0,dp(5)); stageContainer.addView(panel,pp);
            panel.addView(text("Passaggio "+(i+1)+"   Ø "+fmt(d[i],2)+" → Ø "+fmt(d[i+1],2)+" mm",17,true));
            TextView detail=text("Riduzione: "+fmt(reductions[i]*100,2)+"%   •   Allungamento: "+fmt((1/(1-reductions[i])-1)*100,1)+"%"+(fixed[i]?"   •   fissata":""),14,false);
            panel.addView(detail);
            if(reductions[i]>.34+1e-9) high=true;
            SeekBar slider=new SeekBar(this); slider.setMax(820); // 18% to 100%, increments 0.1%
            slider.setProgress(Math.max(0,Math.min(820,(int)Math.round(reductions[i]*1000)-180)));
            panel.addView(slider);
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStartTrackingTouch(SeekBar s) {}
                @Override public void onProgressChanged(SeekBar s,int p,boolean user) {
                    if(user) detail.setText("Riduzione scelta: "+fmt((180+p)/10.0,1)+"% (rilascia per ricalcolare)");
                }
                @Override public void onStopTrackingTouch(SeekBar s) {
                    double v=(180+s.getProgress())/1000.0;
                    if(v>=1) v=.999;
                    double[] old=reductions.clone(); boolean[] oldFixed=fixed.clone();
                    if(!solve(index,v)) {
                        reductions=old; fixed=oldFixed;
                        alert("Combinazione impossibile","Non è possibile mantenere il diametro finale con questa riduzione e il minimo del 18% sugli altri passaggi. Riduci il valore o sblocca un altro passaggio.");
                    }
                    refreshStages(stageContainer);
                }
            });
            if(fixed[i]) {
                Button unlock=new Button(this); unlock.setText("Sblocca passaggio "+(i+1)); unlock.setAllCaps(false);
                unlock.setOnClickListener(v -> { fixed[index]=false; solve(-1,0); refreshStages(stageContainer); }); panel.addView(unlock);
            }
        }
        double finalCalculated=d[steps];
        double kgHour=Math.PI/4*end*end*0.00785*speed*3600;
        production.setText("Velocità uscita: "+fmt(speed,2)+" m/s\nProduttività teorica: "+fmt(kgHour,0)+" kg/h\nProduttività all'80%: "+fmt(kgHour*.8,0)+" kg/h");
        status.setText("Diametro finale calcolato: "+fmt(finalCalculated,2)+" mm"+(high?"\n⚠ Attenzione: almeno una riduzione supera il 34%.":""));
        status.setTextColor(high?Color.rgb(168,82,0):green);
        updating=false;
    }
    private void save() {
        SharedPreferences.Editor e=getSharedPreferences("saved",MODE_PRIVATE).edit();
        e.putFloat("start",(float)start).putFloat("end",(float)end).putFloat("speed",(float)speed).putInt("steps",steps);
        for(int i=0;i<7;i++) { e.putFloat("r"+i,i<steps?(float)reductions[i]:0); e.putBoolean("f"+i,i<steps&&fixed[i]); }
        e.apply(); alert("Salvato","Calcolo memorizzato sul dispositivo.");
    }
    private void load() {
        SharedPreferences p=getSharedPreferences("saved",MODE_PRIVATE);
        if(!p.contains("start")) { alert("Nessun salvataggio","Non hai ancora salvato un calcolo."); return; }
        start=p.getFloat("start",5.5f); end=p.getFloat("end",2.5f); speed=p.getFloat("speed",10);
        steps=p.getInt("steps",5); reductions=new double[steps]; fixed=new boolean[steps];
        for(int i=0;i<steps;i++) { reductions[i]=p.getFloat("r"+i,0); fixed[i]=p.getBoolean("f"+i,false); }
        showResults();
    }
    @Override public void onBackPressed() { showInput(); }
}
