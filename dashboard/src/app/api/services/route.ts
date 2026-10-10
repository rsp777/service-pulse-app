import { NextResponse } from 'next/server';
import pool from '@/lib/db';

export async function GET() {
  try {
    const servicesResult = await pool.query('SELECT * FROM services');
    const services = servicesResult.rows;

    const servicesWithStatus = await Promise.all(
      services.map(async (service: any) => {
        const metricResult = await pool.query(
          'SELECT value, metric_type FROM metrics WHERE service_id = $1 ORDER BY timestamp DESC LIMIT 1',
          [service.id]
        );
        const lastMetric = metricResult.rows[0];
        return {
          ...service,
          last_value: lastMetric?.value || 0,
          last_status: lastMetric?.metric_type || 'UNKNOWN',
        };
      })
    );

    return NextResponse.json(servicesWithStatus);
  } catch (error: any) {
    console.error('API Error:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
