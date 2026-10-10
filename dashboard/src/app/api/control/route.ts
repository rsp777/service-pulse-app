import { NextResponse } from 'next/server';

export async function POST(
  request: Request
) {
  try {
    const { serviceId, action } = await request.json();

    if (!serviceId || !action) {
      return NextResponse.json({ error: 'Missing serviceId or action' }, { status: 400 });
    }

    // Forward the request to the Java Collector
    const response = await fetch(`http://service-collector:8080/control/execute/${serviceId}/${action}`, {
      method: 'POST',
    });

    const data = await response.text();
    
    if (!response.ok) {
      return NextResponse.json({ error: data }, { status: response.status });
    }

    return NextResponse.json({ message: data });
  } catch (error: any) {
    console.error('Control Proxy Error:', error);
    return NextResponse.json({ error: 'Collector unreachable' }, { status: 502 });
  }
}
