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
  const [logs, setLogs] = useState<string>("");
  const [selectedService, setSelectedService] = useState<any>(null);
  const [isConfigOpen, setIsConfigOpen] = useState(false);
  const [configForm, setConfigForm] = useState({ start_script: "", stop_script: "", restart_script: "", logs_path: "" });

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

  const fetchLogs = async (serviceId: number) => {
    try {
      const res = await fetch("/api/service/" + serviceId + "/logs");
      if (res.ok) {
        const text = await res.text();
        setLogs(text);
      } else {
        setLogs("Error fetching logs: " + res.statusText);
      }
    } catch (err) {
      setLogs("Failed to connect to Collector for logs.");
    }
  };

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

  const handleDeleteService = async (serviceId: number) => {
    if (!confirm("Delete this service?")) return;
    try {
      const res = await fetch("/api/service/" + serviceId, { method: "DELETE" });
      if (res.ok) {
        alert("Service deleted");
        fetchDetails();
      }
    } catch (err) {
      alert("Delete failed");
    }
  };

  const saveConfig = async () => {
    try {
      const res = await fetch("/api/service/" + selectedService.id, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(configForm),
      });
      if (res.ok) {
        alert("Config updated");
        setIsConfigOpen(false);
      }
    } catch (err) {
      alert("Update failed");
    }
  };

  useEffect(() => {
    fetchDetails();
    fetchResources();
    const interval = setInterval(() => {
      fetchDetails();
      fetchResources();
    }, 5000);
    return () => clearInterval(interval);
  }, [params.id]);

  if (!data) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-zinc-400">
        <div className="text-xl animate-pulse">Loading...</div>
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
      <div className="max-w-6xl mx-auto">
        <button onClick={() => router.push("/")} className="mb-8 text-zinc-500 hover:text-white transition-colors flex items-center gap-2">
          ← Back to Dashboard
        </button>

        <header className="flex justify-between items-center mb-12">
          <div className="text-left">
            <h1 className="text-4xl font-bold tracking-tight">{data.server.hostname}</h1>
            <p className="text-zinc-500 mt-2">{data.server.ip} • {data.server.os_type}</p>
          </div>
          <div className="flex items-center gap-4">
            <div className={`w-12 h-12 rounded-full ${getHealthColor()} shadow-lg shadow-current animate-pulse`} title="Aggregate Server Health" />
            <button onClick={async () => { if(confirm("Delete server?")) { await fetch("/api/server/" + params.id, {method: "DELETE"}); router.push("/"); } }} className="text-red-900 hover:text-red-500 text-xs font-bold uppercase">Delete Server</button>
          </div>
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

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
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
                          <span className={`font-mono font-bold ${cpu > 80 ? "text-red-500" : "text-green-400"}`}>{cpu.toFixed(1)}%</span>
                        </div>
                        <div className="flex flex-col items-end">
                          <span className="text-zinc-500 text-[10px] uppercase font-bold">RAM</span>
                          <span className={`font-mono font-bold ${ram > 80 ? "text-red-500" : "text-green-400"}`}>{ram.toFixed(1)}%</span>
                        </div>
                      </div>

                      <div className="flex gap-2 ml-4 pl-4 border-l border-zinc-800">
                        <button onClick={() => handleControl(service.id, "start")} className="px-3 py-1 rounded bg-zinc-800 hover:bg-green-900 text-xs font-bold transition-colors">START</button>
                        <button onClick={() => handleControl(service.id, "stop")} className="px-3 py-1 rounded bg-zinc-800 hover:bg-red-900 text-xs font-bold transition-colors">STOP</button>
                        <button onClick={() => handleControl(service.id, "restart")} className="px-3 py-1 rounded bg-zinc-800 hover:bg-blue-900 text-xs font-bold transition-colors">RESTART</button>
                        <button onClick={() => { setSelectedService(service); setConfigForm({ start_script: service.start_script || "", stop_script: service.stop_script || "", restart_script: service.restart_script || "", logs_path: service.logs_path || "" }); setIsConfigOpen(true); }} className="px-3 py-1 rounded bg-zinc-700 hover:bg-zinc-600 text-xs font-bold transition-colors">⚙️</button>
                        <button onClick={() => { setSelectedService(service); fetchLogs(service.id); }} className="px-3 py-1 rounded bg-zinc-800 hover:bg-zinc-700 text-xs font-bold transition-colors">LOGS</button>
                        <button onClick={() => handleDeleteService(service.id)} className="px-3 py-1 rounded bg-zinc-800 hover:bg-red-950 text-xs font-bold transition-colors text-red-500">DEL</button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="lg:col-span-1">
            {selectedService && (
              <div className="p-6 rounded-2xl bg-zinc-900 border border-zinc-800 h-full flex flex-col">
                <h3 className="text-lg font-bold mb-4 flex items-center justify-between">
                  <span>Logs: {selectedService.name}</span>
                  <button onClick={() => setSelectedService(null)} className="text-zinc-500 hover:text-white">✕</button>
                </h3>
                <div className="flex-1 bg-black rounded-lg p-4 font-mono text-xs text-green-500 overflow-auto max-h-[500px] whitespace-pre-wrap border border-zinc-800">
                  {logs || "No logs available..."}
                </div>
              </div>
            )}
            {!selectedService && (
              <div className="p-6 rounded-2xl bg-zinc-900 border border-zinc-800 h-full flex items-center justify-center text-zinc-500 italic text-center">
                Select a service to view real-time logs
              </div>
            )}
          </div>
        </div>
      </div>

      {isConfigOpen && selectedService && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-zinc-900 border border-zinc-800 p-8 rounded-2xl max-w-md w-full">
            <h2 className="text-xl font-bold mb-6">Configure {selectedService.name}</h2>
            <div className="space-y-4">
              <div>
                <label className="text-xs text-zinc-500 uppercase font-bold block mb-1">Start Script Path</label>
                <input type="text" className="w-full bg-black border border-zinc-800 p-2 rounded text-sm font-mono" value={configForm.start_script} onChange={e => setConfigForm({...configForm, start_script: e.target.value})} />
              </div>
              <div>
                <label className="text-xs text-zinc-500 uppercase font-bold block mb-1">Stop Script Path</label>
                <input type="text" className="w-full bg-black border border-zinc-800 p-2 rounded text-sm font-mono" value={configForm.stop_script} onChange={e => setConfigForm({...configForm, stop_script: e.target.value})} />
              </div>
              <div>
                <label className="text-xs text-zinc-500 uppercase font-bold block mb-1">Restart Script Path</label>
                <input type="text" className="w-full bg-black border border-zinc-800 p-2 rounded text-sm font-mono" value={configForm.restart_script} onChange={e => setConfigForm({...configForm, restart_script: e.target.value})} />
              </div>
              <div>
                <label className="text-xs text-zinc-500 uppercase font-bold block mb-1">Logs Path</label>
                <input type="text" className="w-full bg-black border border-zinc-800 p-2 rounded text-sm font-mono" value={configForm.logs_path} onChange={e => setConfigForm({...configForm, logs_path: e.target.value})} />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-8 pt-6 border-t border-zinc-800">
              <button onClick={() => setIsConfigOpen(false)} className="px-4 py-2 text-sm text-zinc-400 hover:text-white">Cancel</button>
              <button onClick={saveConfig} className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded text-sm font-bold transition-colors">Save Changes</button>
            </div>
          </div>
        </div>
      )}
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
