"use client";

import { useEffect, useState, Suspense } from "react";
import { useParams, useRouter } from "next/navigation";

interface ServerData {
  server: {
    id: number;
    hostname: string;
    ip: string;
    os_type: string;
    status: string;
  };
  services: any[];
  lastHealth: {
    value: number;
    metric_type: string;
    timestamp: string;
  } | null;
}

interface ResourceMetric {
  name: string;
  value: number;
  metric_type: "CPU" | "RAM";
  timestamp: string;
}

function ServerDetailContent() {
  const params = useParams();
  const router = useRouter();
  const [data, setData] = useState<ServerData | null>(null);
  const [resourceMetrics, setResourceMetrics] = useState<ResourceMetric[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchDetails = async () => {
    try {
      const res = await fetch("/api/server/" + params.id);
      if (!res.ok) throw new Error("Server not found");
      const result = await res.json();
      setData(result);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchResources = async () => {
    try {
      const res = await fetch("/api/server/" + params.id + "/metrics");
      if (res.ok) {
        const result = await res.json();
        setResourceMetrics(result);
      }
    } catch (err) {
      console.error("Resource fetch error:", err);
    }
  };

  useEffect(() => {
    fetchDetails();
    fetchResources();
    const interval = setInterval(() => {
      fetchDetails();
      fetchResources();
    }, 5000);
    setLoading(false);
    return () => clearInterval(interval);
  }, [params.id]);

  const handleControl = async (serviceId: number, action: string) => {
    try {
      const res = await fetch("/api/control", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ serviceId, action }),
      });
      const result = await res.json();
      if (res.ok) {
        alert("Success: " + result.message);
      } else {
        alert("Error: " + result.error);
      }
    } catch (err) {
      alert("Network error occurred");
    }
  };

  if (loading && !data) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-zinc-400">
        <div className="text-xl animate-pulse">Loading Server Details...</div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-zinc-400">
        <div>Server not found. <button onClick={() => router.push("/")} className="text-blue-500 underline">Go Back</button></div>
      </div>
    );
  }

  const getHealthColor = () => {
    if (!data.lastHealth) return "bg-zinc-500";
    const val = data.lastHealth.value;
    if (val > 2.0) return "bg-red-500"; 
    if (val > 1.0) return "bg-yellow-500"; 
    return "bg-green-500";
  };

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100 p-8">
      <div className="max-w-4xl mx-auto">
        <button onClick={() => router.push("/")} className="mb-8 text-zinc-500 hover:text-white transition-colors flex items-center gap-2">
          ← Back to Dashboard
        </button>

        <header className="flex justify-between items-center mb-12">
          <div className="text-left">
            <h1 className="text-4xl font-bold tracking-tight">{data.server.hostname}</h1>
            <p className="text-zinc-500 mt-2">{data.server.ip} • {data.server.os_type}</p>
          </div>
          <div className={`w-12 h-12 rounded-full ${getHealthColor()} shadow-lg shadow-current animate-pulse`} title="Aggregate Server Health" />
        </header>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          <div className="p-6 rounded-2xl bg-zinc-900 border border-zinc-800">
            <span className="text-zinc-500 text-sm block mb-1">Server Status</span>
            <span className="text-2xl font-bold uppercase">{data.server.status || "Active"}</span>
          </div>
          <div className="p-6 rounded-2xl bg-zinc-900 border border-zinc-800">
            <span className="text-zinc-500 text-sm block mb-1">Current Latency</span>
            <span className="text-2xl font-bold font-mono">
              {data.lastHealth ? data.lastHealth.value.toFixed(2) : "N/A"}ms
            </span>
          </div>
          <div className="p-6 rounded-2xl bg-zinc-900 border border-zinc-800">
            <span className="text-zinc-500 text-sm block mb-1">Associated Services</span>
            <span className="text-2xl font-bold">{data.services.length}</span>
          </div>
        </div>

        <h2 className="text-2xl font-bold mb-6">Service Resource Consumption & Control</h2>
        <div className="grid grid-cols-1 gap-4">
          {data.services.map((service) => {
            const cpu = resourceMetrics.find(m => m.name === service.name && m.metric_type === "CPU")?.value || 0;
            const ram = resourceMetrics.find(m => m.name === service.name && m.metric_type === "RAM")?.value || 0;

            return (
              <div key={service.id} className="p-4 rounded-xl bg-zinc-900 border border-zinc-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="text-left">
                    <span className="font-semibold block">{service.name}</span>
                    <span className="text-zinc-500 text-xs font-mono">{service.url}</span>
                  </div>
                  <span className="text-[10px] uppercase font-bold px-2 py-1 rounded bg-zinc-800 text-zinc-400">{service.type}</span>
                </div>
                
                <div className="flex items-center gap-6">
                  <div className="flex gap-6">
                    <div className="flex flex-col items-end">
                      <span className="text-zinc-500 text-[10px] uppercase font-bold">CPU</span>
                      <span className={`font-mono font-bold ${cpu > 80 ? 'text-red-500' : 'text-green-400'}`}>{cpu.toFixed(1)}%</span>
                    </div>
                    <div className="flex flex-col items-end">
                      <span className="text-zinc-500 text-[10px] uppercase font-bold">RAM</span>
                      <span className={`font-mono font-bold ${ram > 80 ? 'text-red-500' : 'text-green-400'}`}>{ram.toFixed(1)}%</span>
                    </div>
                  </div>

                  <div className="flex gap-2 ml-4 pl-4 border-l border-zinc-800">
                    <button onClick={() => handleControl(service.id, 'start')} className="px-3 py-1 rounded bg-zinc-800 hover:bg-green-900 text-xs font-bold transition-colors">START</button>
                    <button onClick={() => handleControl(service.id, 'stop')} className="px-3 py-1 rounded bg-zinc-800 hover:bg-red-900 text-xs font-bold transition-colors">STOP</button>
                    <button onClick={() => handleControl(service.id, 'restart')} className="px-3 py-1 rounded bg-zinc-800 hover:bg-blue-900 text-xs font-bold transition-colors">RESTART</button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </main>
  );
}

export default function ServerDetail() {
  return (
    <Suspense fallback={<div className="flex h-screen items-center justify-center bg-zinc-950 text-zinc-400"><div className="text-xl animate-pulse">Loading...</div></div>}>
      <ServerDetailContent />
    </Suspense>
  );
}
