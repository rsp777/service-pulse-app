import { NextResponse } from 'next/server';
import pool from '@/lib/db';

export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  try {
    await pool.query('DELETE FROM services WHERE id = $1', [id]);
    return NextResponse.json({ message: 'Service deleted successfully' });
  } catch (error: any) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  try {
    const body = await request.json();
    const { start_script, stop_script, restart_script, logs_path } = body;

    const query = `
      UPDATE services 
      SET start_script = COALESCE($1, start_script), 
          stop_script = COALESCE($2, stop_script), 
          restart_script = COALESCE($3, restart_script), 
          logs_path = COALESCE($4, logs_path)
      WHERE id = $5
    `;
    await pool.query(query, [start_script, stop_script, restart_script, logs_path, id]);
    
    return NextResponse.json({ message: 'Service configuration updated' });
  } catch (error: any) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
