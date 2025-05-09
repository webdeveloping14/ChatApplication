package com.example.chatapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.chatapplication.R;

import java.util.ArrayList;
import java.util.Random;

public class ParticleView extends View {

    private ArrayList<Particle> particles = new ArrayList<>();
    private Paint particlePaint;
    private Random random = new Random();

    // Configuration properties
    private int particleColor;
    private int particleCount;
    private float particleMinSize;
    private float particleMaxSize;
    private float particleSpeed;

    // Connection lines between particles when close
    private boolean drawConnections = true;
    private float connectionDistance = 150f;
    private int connectionColor;

    public ParticleView(Context context) {
        super(context);
        init(null);
    }

    public ParticleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ParticleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        // Default values
        particleColor = Color.WHITE;
        particleCount = 30;
        particleMinSize = 2f;
        particleMaxSize = 6f;
        particleSpeed = 2.0f;
        connectionColor = Color.WHITE;

        if (attrs != null) {
            // Get custom attributes from XML
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ParticleView);
            particleColor = a.getColor(R.styleable.ParticleView_particleColor, particleColor);
            particleCount = a.getInt(R.styleable.ParticleView_particleCount, particleCount);
            particleMinSize = a.getDimension(R.styleable.ParticleView_particleMinSize, particleMinSize);
            particleMaxSize = a.getDimension(R.styleable.ParticleView_particleMaxSize, particleMaxSize);
            particleSpeed = a.getFloat(R.styleable.ParticleView_particleSpeed, particleSpeed);
            drawConnections = a.getBoolean(R.styleable.ParticleView_drawConnections, drawConnections);
            connectionDistance = a.getDimension(R.styleable.ParticleView_connectionDistance, connectionDistance);
            connectionColor = a.getColor(R.styleable.ParticleView_connectionColor, particleColor);
            a.recycle();
        }

        particlePaint = new Paint();
        particlePaint.setAntiAlias(true);
        particlePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        createParticles();
    }

    private void createParticles() {
        particles.clear();

        for (int i = 0; i < particleCount; i++) {
            float x = random.nextFloat() * getWidth();
            float y = random.nextFloat() * getHeight();
            float size = particleMinSize + random.nextFloat() * (particleMaxSize - particleMinSize);
            float dirX = -1f + random.nextFloat() * 2f;
            float dirY = -1f + random.nextFloat() * 2f;
            int alpha = 50 + random.nextInt(206);
            particles.add(new Particle(x, y, size, dirX, dirY, alpha));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawConnections) {
            Paint connectionPaint = new Paint(particlePaint);
            connectionPaint.setStyle(Paint.Style.STROKE);
            connectionPaint.setStrokeWidth(1f);

            for (int i = 0; i < particles.size(); i++) {
                Particle p1 = particles.get(i);
                for (int j = i + 1; j < particles.size(); j++) {
                    Particle p2 = particles.get(j);
                    float dx = p1.x - p2.x;
                    float dy = p1.y - p2.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance < connectionDistance) {
                        float alpha = 1f - (distance / connectionDistance);
                        connectionPaint.setColor(adjustAlpha(connectionColor, (int) (alpha * 80)));
                        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, connectionPaint);
                    }
                }
            }
        }

        for (Particle particle : particles) {
            particlePaint.setColor(adjustAlpha(particleColor, particle.alpha));
            canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint);
            particle.x += particle.dirX * particleSpeed;
            particle.y += particle.dirY * particleSpeed;
            if (particle.x < 0 || particle.x > getWidth()) {
                particle.dirX *= -1;
                particle.x = Math.max(0, Math.min(getWidth(), particle.x));
            }
            if (particle.y < 0 || particle.y > getHeight()) {
                particle.dirY *= -1;
                particle.y = Math.max(0, Math.min(getHeight(), particle.y));
            }
        }

        invalidate();
    }

    private int adjustAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static class Particle {
        float x, y;
        float size;
        float dirX, dirY;
        int alpha;

        Particle(float x, float y, float size, float dirX, float dirY, int alpha) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.dirX = dirX;
            this.dirY = dirY;
            this.alpha = alpha;
        }
    }
}