"use client";

import { useEffect, useState } from "react";

interface Service {
  id: number;
  name: string;
  url: string;
  type: string;
  last_value: number;
  last_status: string;
}

export default function Dashboard() {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = async () => {
    try {
      const res = await fetch("/api/services");
      if (!res.ok) throw new Error(`API Error: ${res.statusText}`);
      const data = await res.json();
      setServices(data);
      setError(null);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 5000);
    return () => clearInterval(interval);
  }, []);

  if (loading && services.length === 0) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-zinc-400">
        <div className="text-xl animate-pulse">Loading System Health...</div>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100 p-8">
      <div className="max-w-6xl mx-auto">
        <header className="flex justify-between items-center mb-12">
          <div>
            <h1 className="text-4xl font-bold tracking-tight">System Monitor</h1>
            <p className="text-zinc-500 mt-2">Real-time agentless service health tracking</p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-zinc-900 border border-zinc-800 text-xs text-zinc-400">
            <div className="w-2 h-2 rounded-full bg-green-500 animate-ping" />
            Live Updates Every 5s
          </div>
        </header>
        {error && (
          <div className="mb-8 p-4 rounded-lg bg-red-900/20 border border-red-500/50 text-red-400">
            <strong className="font-bold">Error:</strong> {error}
          </div>
        )}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {services.map((service) => {
            const isUp = service.last_status === "UP" || service.last_status.startsWith("UP");
            return (
              <div key={service.id} className="p-6 rounded-2xl bg-zinc-900 border border-zinc-800 hover:border-zinc-700 transition-all group">
                <div className="flex justify-between items-start mb-4">
                  <h3 className="text-lg font-semibold group-hover:text-white transition-colors cursor-pointer" onClick={() => {
                    window.location.href = `/server/1`;
                  }}>{service.name}</h3>
                  <span className={`px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${isUp ? "bg-green-500/10 text-green-500" : "bg-red-500/10 text-red-500"}`}>
                    {service.last_status}
                  </span>
                </div>
                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-zinc-500">URL</span>
                    <span className="text-zinc-300 font-mono truncate ml-4">{service.url}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-zinc-500">Type</span>
                    <span className="text-zinc-300 uppercase">{service.type}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-zinc-500">Latency</span>
                    <span className={`font-mono ${service.last_value > 500 ? "text-yellow-500" : "text-zinc-300"}`}>
                      {service.last_value.toFixed(2)}ms
                    </span>
                  </div>
                </div>
                <div className="mt-6 h-1 w-full bg-zinc-800 rounded-full overflow-hidden">
                  <div className={`h-full transition-all duration-1000 ${isUp ? "bg-green-500" : "bg-red-500"}`} style={{ width: isUp ? "100%" : "20%" }} />
                </div>
              </div>
            );
          })}
        </div>
        {services.length === 0 && !error && (
          <div className="text-center py-20 text-zinc-600">No services found in the monitoring list.</div>
        )}
      </div>
    </main>
  );
}

