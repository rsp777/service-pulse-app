import { NextResponse } from 'next/server';
import pool from '@/lib/db';

export async function GET(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id: serverId } = await params;
  try {
    const serverResult = await pool.query('SELECT * FROM servers WHERE id = $1', [serverId]);
    const server = serverResult.rows[0];

    if (!server) {
      return NextResponse.json({ error: 'Server not found' }, { status: 404 });
    }

    const servicesResult = await pool.query('SELECT * FROM services WHERE server_id = $1', [serverId]);
    const services = servicesResult.rows;

    const metricsResult = await pool.query(
      `SELECT m.value, m.metric_type, m.timestamp 
       FROM metrics m 
       JOIN services s ON m.service_id = s.id 
       WHERE s.server_id = $1 AND m.metric_type LIKE 'UP%' 
       ORDER BY m.timestamp DESC LIMIT 1`, 
      [serverId]
    );
    const lastHealth = metricsResult.rows[0];

    return NextResponse.json({
      server,
      services,
      lastHealth
    });
  } catch (error: any) {
    console.error('API Error:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}

export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  try {
    // Assuming services table has FK with ON DELETE CASCADE, 
    // otherwise we'd delete services first.
    await pool.query('DELETE FROM servers WHERE id = $1', [id]);
    return NextResponse.json({ message: 'Server deleted successfully' });
  } catch (error: any) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
