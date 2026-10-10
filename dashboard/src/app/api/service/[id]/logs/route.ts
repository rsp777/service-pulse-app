import { NextResponse } from 'next/server';

export async function GET(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id: serviceId } = await params;
  try {
    const collectorUrl = process.env.COLLECTOR_URL || 'http://service-collector:8080';
    const response = await fetch(`${collectorUrl}/logs/${serviceId}`, {
      method: 'GET',
    });

    if (!response.ok) {
      return NextResponse.json({ error: 'Failed to fetch logs from collector' }, { status: response.status });
    }

    const logs = await response.text();
    return new NextResponse(logs, {
      status: 200,
      headers: { 'Content-Type': 'text/plain' },
    });
  } catch (error: any) {
    console.error('Logs Proxy Error:', error);
    return NextResponse.json({ error: 'Collector unreachable' }, { status: 502 });
  }
}
