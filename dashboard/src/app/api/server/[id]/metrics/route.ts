import { NextResponse } from 'next/server';
import pool from '@/lib/db';

export async function GET(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id: serverId } = await params;
  try {
    // Fetch services for this server to map service_id to name
    const servicesResult = await pool.query('SELECT id, name FROM services WHERE server_id = $1', [serverId]);
    const services = servicesResult.rows;

    // Fetch latest CPU and RAM metrics for all services on this server
    const query = `
      SELECT DISTINCT ON (s.id, m.metric_type) 
             s.name, m.value, m.metric_type, m.timestamp
      FROM metrics m
      JOIN services s ON m.service_id = s.id
      WHERE s.server_id = $1 AND m.metric_type IN ('CPU', 'RAM')
      ORDER BY s.id, m.metric_type, m.timestamp DESC
    `;
    const metricsResult = await pool.query(query, [serverId]);
    
    return NextResponse.json(metricsResult.rows);
  } catch (error: any) {
    console.error('Metrics API Error:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
