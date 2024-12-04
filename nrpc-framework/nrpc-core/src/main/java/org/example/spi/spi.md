spi --> 主动发现某一服务 service provider interface
jdbc --> 自动发现jdbc的驱动

1、默认配置
compress code(1) type(gzip) impl(GzipCompressor)

2、通过spi加载 ->
configuration.setCompressor(compressor)

3、通过xml进行配置 ->
configuration.setCompressor()
configuration.setCompressorType

-- 具体传输时获取
CompressorFactory.getCompressor(
NrpcBootstrap.getInstance().getConfiguration().getCompressType())
.getCode()